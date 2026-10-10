package com.graphics.juego.trafico;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.graphics.juego.ciudad.MapaCiudad;

/**
 * Vehiculo que circula solo por las calles del mapa.
 *
 * Ruta: avanza celda por celda. Al entrar en una celda de calle elige por donde salir
 * (seguir recto o girar hacia otra calle vecina; nunca hacia una manzana). El recorrido
 * dentro de la celda va por el carril derecho:
 *   - recto: segmento de borde a borde,
 *   - giro: curva de Bezier cuadratica cuyo punto de control es el cruce de los dos carriles,
 *   - vuelta en U (solo en calles sin salida): Bezier cubica.
 * Todos los puntos quedan dentro de celdas de calle, asi nunca atraviesa edificios ni sale
 * del mapa. Mantiene posicion, orientacion (tangente de la curva) y velocidad propia.
 */
public class VehiculoAutonomo {

    /** Distancia del centro de la calle al centro del carril derecho. */
    public static final float CARRIL = 2.5f;
    public static final float VELOCIDAD_RECTA = 8.5f;
    public static final float VELOCIDAD_CURVA = 4.5f;
    private static final float ACELERACION = 5;
    private static final float FRENADO = 9;

    private final MapaCiudad mapa;
    private final Random azar;
    private final long semilla;
    public final float[] color;

    private final int filaInicial;
    private final int columnaInicial;
    private final int direccionInicial;

    // Estado
    private int fila;
    private int columna;
    private int direccionEntrada;
    private int direccionSalida;
    private final List<float[]> puntos = new ArrayList<>();
    private float largo;
    private float recorrido;
    private boolean enCurva;

    public float x;
    public float z;
    public float angulo;
    public float velocidad;

    VehiculoAutonomo(MapaCiudad mapa, int fila, int columna, int direccion, long semilla, float[] color) {
        this.mapa = mapa;
        this.semilla = semilla;
        this.azar = new Random(semilla);
        this.color = color;
        this.filaInicial = fila;
        this.columnaInicial = columna;
        this.direccionInicial = direccion;
        reiniciar();
    }

    public void reiniciar() {
        azar.setSeed(semilla);
        fila = filaInicial;
        columna = columnaInicial;
        direccionEntrada = direccionInicial;
        velocidad = 0;
        prepararCelda();
        recorrido = largo / 2; // empieza en medio de su celda
        colocar();
    }

    public int fila() {
        return fila;
    }

    public int columna() {
        return columna;
    }

    public int direccionSalida() {
        return direccionSalida;
    }

    public boolean enCurva() {
        return enCurva;
    }

    public float frenteX() {
        return -(float) Math.sin(angulo);
    }

    public float frenteZ() {
        return -(float) Math.cos(angulo);
    }

    /** Distancia que falta para salir de la celda actual. */
    public float restanteEnCelda() {
        return largo - recorrido;
    }

    /** Fila y columna de la siguiente celda de la ruta. */
    public int filaSiguiente() {
        return fila + MapaCiudad.DF[direccionSalida];
    }

    public int columnaSiguiente() {
        return columna + MapaCiudad.DC[direccionSalida];
    }

    /**
     * Avanza deltaTime segundos.
     * @param limiteVelocidad velocidad maxima permitida en este cuadro (semaforo u otro auto delante)
     */
    void actualizar(float deltaTime, float limiteVelocidad) {
        float deseada = Math.min(enCurva ? VELOCIDAD_CURVA : VELOCIDAD_RECTA, limiteVelocidad);
        if (velocidad < deseada) {
            velocidad = Math.min(deseada, velocidad + ACELERACION * deltaTime);
        } else {
            velocidad = Math.max(deseada, velocidad - FRENADO * deltaTime);
        }
        recorrido += velocidad * deltaTime;
        while (recorrido >= largo) {
            recorrido -= largo;
            fila = filaSiguiente();
            columna = columnaSiguiente();
            direccionEntrada = direccionSalida;
            prepararCelda();
        }
        colocar();
    }

    // ===================== Ruta =====================

    /** Elige la salida de la celda actual y construye los puntos del carril. */
    private void prepararCelda() {
        direccionSalida = elegirSalida();
        puntos.clear();
        float cx = mapa.centroX(columna);
        float cz = mapa.centroZ(fila);
        float mitad = MapaCiudad.CELDA / 2;
        int dIn = direccionEntrada;
        int dOut = direccionSalida;
        // Punto de entrada (borde por donde llega) y de salida, ambos en el carril derecho
        float[] entrada = { cx + derX(dIn) * CARRIL - dirX(dIn) * mitad, cz + derZ(dIn) * CARRIL - dirZ(dIn) * mitad };
        float[] salida = { cx + derX(dOut) * CARRIL + dirX(dOut) * mitad, cz + derZ(dOut) * CARRIL + dirZ(dOut) * mitad };
        enCurva = dIn != dOut;
        if (!enCurva) {
            puntos.add(entrada);
            puntos.add(salida);
        } else if (dOut == (dIn + 2) % 4) {
            // Vuelta en U: dos puntos de control hacia el fondo de la celda
            float[] c1 = { entrada[0] + dirX(dIn) * 7, entrada[1] + dirZ(dIn) * 7 };
            float[] c2 = { salida[0] - dirX(dOut) * 7, salida[1] - dirZ(dOut) * 7 };
            for (int i = 0; i <= 12; i++) {
                float t = i / 12f;
                float a = (1 - t) * (1 - t) * (1 - t);
                float b = 3 * (1 - t) * (1 - t) * t;
                float c = 3 * (1 - t) * t * t;
                float d = t * t * t;
                puntos.add(new float[] {
                        a * entrada[0] + b * c1[0] + c * c2[0] + d * salida[0],
                        a * entrada[1] + b * c1[1] + c * c2[1] + d * salida[1] });
            }
        } else {
            // Giro: el control es donde se cruzan el carril de entrada y el de salida
            float[] control = { cx + derX(dIn) * CARRIL + derX(dOut) * CARRIL, cz + derZ(dIn) * CARRIL + derZ(dOut) * CARRIL };
            for (int i = 0; i <= 10; i++) {
                float t = i / 10f;
                float a = (1 - t) * (1 - t);
                float b = 2 * (1 - t) * t;
                float c = t * t;
                puntos.add(new float[] {
                        a * entrada[0] + b * control[0] + c * salida[0],
                        a * entrada[1] + b * control[1] + c * salida[1] });
            }
        }
        largo = 0;
        for (int i = 1; i < puntos.size(); i++) {
            largo += distancia(puntos.get(i - 1), puntos.get(i));
        }
    }

    /** Recto tiene doble probabilidad; nunca vuelve atras salvo en un callejon sin salida. */
    private int elegirSalida() {
        int[] opciones = new int[4];
        int[] pesos = new int[4];
        int total = 0;
        int cantidad = 0;
        for (int giro = -1; giro <= 1; giro++) {
            int d = (direccionEntrada + giro + 4) % 4;
            if (mapa.esCalle(fila + MapaCiudad.DF[d], columna + MapaCiudad.DC[d])) {
                opciones[cantidad] = d;
                pesos[cantidad] = giro == 0 ? 2 : 1;
                total += pesos[cantidad];
                cantidad++;
            }
        }
        if (cantidad == 0) {
            return (direccionEntrada + 2) % 4;
        }
        int r = azar.nextInt(total);
        for (int i = 0; i < cantidad; i++) {
            r -= pesos[i];
            if (r < 0) {
                return opciones[i];
            }
        }
        return opciones[cantidad - 1];
    }

    /** Ubica el vehiculo en el punto "recorrido" de la polilinea; la orientacion es la tangente. */
    private void colocar() {
        float resto = recorrido;
        for (int i = 1; i < puntos.size(); i++) {
            float[] a = puntos.get(i - 1);
            float[] b = puntos.get(i);
            float tramo = distancia(a, b);
            if (resto <= tramo || i == puntos.size() - 1) {
                float t = tramo > 0 ? Math.min(1, resto / tramo) : 0;
                x = a[0] + (b[0] - a[0]) * t;
                z = a[1] + (b[1] - a[1]) * t;
                // frente = (-sin a, -cos a) = direccion del tramo
                angulo = (float) Math.atan2(-(b[0] - a[0]), -(b[1] - a[1]));
                return;
            }
            resto -= tramo;
        }
    }

    private static float distancia(float[] a, float[] b) {
        return (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
    }

    // Direccion d: 0 norte, 1 este, 2 sur, 3 oeste. La derecha de d es d + 1.
    private static float dirX(int d) {
        return MapaCiudad.DC[d];
    }

    private static float dirZ(int d) {
        return MapaCiudad.DF[d];
    }

    private static float derX(int d) {
        return MapaCiudad.DC[(d + 1) % 4];
    }

    private static float derZ(int d) {
        return MapaCiudad.DF[(d + 1) % 4];
    }
}
