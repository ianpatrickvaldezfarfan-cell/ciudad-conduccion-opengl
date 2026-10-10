package com.graphics.juego.funciones;

import com.graphics.juego.ciudad.MapaCiudad;
import com.graphics.juego.motor.Renderizador;

/**
 * Semaforos animados en los cruces marcados con 'T' en el mapa.
 *
 * Secuencia repetitiva controlada por el tiempo (deltaTime acumulado):
 *   eje norte-sur:  VERDE 5 s -> AMARILLO 2 s -> ROJO 7 s
 *   eje este-oeste: el mismo ciclo desfasado 7 s (esta en rojo mientras el otro avanza)
 * Cada cruce tiene un pequeno desfase propio para que no cambien todos a la vez.
 * El trafico autonomo los respeta; para el jugador son solo visuales.
 */
public class Semaforos {

    public enum Luz {
        ROJO, VERDE, AMARILLO
    }

    public static final float VERDE = 5;
    public static final float AMARILLO = 2;
    public static final float ROJO = 7;
    public static final float CICLO = VERDE + AMARILLO + ROJO;

    private final MapaCiudad mapa;
    private float tiempo;

    public Semaforos(MapaCiudad mapa) {
        this.mapa = mapa;
    }

    public void actualizar(float deltaTime) {
        tiempo = (tiempo + deltaTime) % (CICLO * 1000);
    }

    public void reiniciar() {
        tiempo = 0;
    }

    /** Luz que ve un conductor que circula por el eje indicado en el cruce (fila, columna). */
    public Luz estado(int fila, int columna, boolean ejeNorteSur) {
        float desfase = (fila * 3 + columna * 5) % 7;
        return luzEn(tiempo + desfase + (ejeNorteSur ? 0 : VERDE + AMARILLO));
    }

    /** Luz del ciclo en el instante t (segundos). */
    public static Luz luzEn(float t) {
        float fase = t % CICLO;
        if (fase < VERDE) {
            return Luz.VERDE;
        }
        if (fase < VERDE + AMARILLO) {
            return Luz.AMARILLO;
        }
        return Luz.ROJO;
    }

    // ===================== Dibujo =====================

    /** Cuatro postes por cruce, uno en cada esquina, mirando al trafico que llega. */
    public void dibujar(Renderizador r) {
        for (int f = 0; f < mapa.filas(); f++) {
            for (int c = 0; c < mapa.columnas(); c++) {
                if (!mapa.tieneSemaforo(f, c)) {
                    continue;
                }
                float x = mapa.centroX(c);
                float z = mapa.centroZ(f);
                float e = MapaCiudad.CELDA / 2 + 0.6f; // sobre la acera de la manzana de la esquina
                Luz ns = estado(f, c, true);
                Luz eo = estado(f, c, false);
                // Esquina sureste: la ve quien sube hacia el norte (mira al sur, +Z)
                poste(r, f + 1, c + 1, x + e, z + e, 0, ns);
                // Esquina noroeste: la ve quien baja hacia el sur (mira al norte, -Z)
                poste(r, f - 1, c - 1, x - e, z - e, (float) Math.PI, ns);
                // Esquina noreste: la ve quien va hacia el oeste (mira al este, +X)
                poste(r, f - 1, c + 1, x + e, z - e, (float) (Math.PI / 2), eo);
                // Esquina suroeste: la ve quien va hacia el este (mira al oeste, -X)
                poste(r, f + 1, c - 1, x - e, z + e, (float) (-Math.PI / 2), eo);
            }
        }
    }

    private void poste(Renderizador r, int filaEsquina, int columnaEsquina, float x, float z, float giro, Luz luz) {
        if (mapa.esCalle(filaEsquina, columnaEsquina)) {
            return; // no hay acera en esa esquina
        }
        r.caja(x, 0.3f + 1.8f, z, 0.16f, 3.6f, 0.16f, 0.16f, 0.17f, 0.19f); // poste
        float s = (float) Math.sin(giro);
        float co = (float) Math.cos(giro);
        float alto = 3.9f;
        r.cajaGirada(x, alto, z, 0.5f, 1.35f, 0.4f, 0.07f, 0.08f, 0.09f, giro); // carcasa
        Luz[] orden = { Luz.ROJO, Luz.AMARILLO, Luz.VERDE };   // de arriba hacia abajo
        float[][] colores = { { 1, 0.08f, 0.05f }, { 1, 0.75f, 0.05f }, { 0.1f, 1, 0.3f } };
        for (int i = 0; i < 3; i++) {
            boolean encendida = orden[i] == luz;
            float brillo = encendida ? 1 : 0.16f;
            float y = alto + 0.4f - i * 0.4f;
            float adelante = 0.23f; // en la cara frontal de la carcasa (local +Z)
            r.emision(encendida);
            r.cajaGirada(x + s * adelante, y, z + co * adelante, 0.28f, 0.28f, 0.06f,
                    colores[i][0] * brillo, colores[i][1] * brillo, colores[i][2] * brillo, giro);
        }
        r.emision(false);
    }
}
