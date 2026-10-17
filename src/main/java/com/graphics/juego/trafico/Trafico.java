package com.graphics.juego.trafico;

import java.util.ArrayList;
import java.util.List;

import com.graphics.juego.ciudad.MapaCiudad;
import com.graphics.juego.conduccion.Auto;
import com.graphics.juego.conduccion.ModeloAuto;
import com.graphics.juego.funciones.Semaforos;
import com.graphics.juego.motor.Renderizador;

/**
 * Trafico autonomo: varios vehiculos que circulan sin intervencion del usuario.
 * Reglas de cada cuadro (todas con deltaTime):
 *   - frenan suavemente antes de un cruce con semaforo en rojo (o amarillo si alcanzan a frenar),
 *   - mantienen distancia con el vehiculo que tienen delante (incluido el jugador),
 *   - si quedan trabados mas de 3 s esperando a otro vehiculo autonomo, avanzan (evita bloqueos).
 */
public class Trafico {

    public static final float RADIO = Auto.RADIO;
    private static final float DISTANCIA_LINEA_PARE = 3.6f; // centro del auto al borde del cruce
    private static final float FRENADO_SUAVE = 6;

    private static final float[][] COLORES = {
            { 0.15f, 0.35f, 0.85f }, // azul
            { 0.95f, 0.78f, 0.10f }, // taxi
            { 0.15f, 0.60f, 0.30f }, // verde
            { 0.90f, 0.90f, 0.92f }, // blanco
            { 0.55f, 0.25f, 0.70f }  // morado
    };

    private final MapaCiudad mapa;
    private final List<VehiculoAutonomo> vehiculos = new ArrayList<>();
    private float[] espera;
    private float[] ignorar;

    public Trafico(MapaCiudad mapa, int cantidad) {
        this.mapa = mapa;
        crearVehiculos(cantidad);
    }

    public List<VehiculoAutonomo> vehiculos() {
        return vehiculos;
    }

    /** Reparte los vehiculos por tramos rectos de calle, lejos de la salida del jugador. */
    private void crearVehiculos(int cantidad) {
        float[] salida = mapa.salida();
        List<int[]> candidatas = new ArrayList<>();
        for (int f = 0; f < mapa.filas(); f++) {
            for (int c = 0; c < mapa.columnas(); c++) {
                char celda = mapa.celda(f, c);
                if (celda != '.' || mapa.esCruce(f, c)) {
                    continue;
                }
                boolean norteSur = mapa.esCalle(f - 1, c) && mapa.esCalle(f + 1, c);
                boolean esteOeste = mapa.esCalle(f, c - 1) && mapa.esCalle(f, c + 1);
                float distancia = (float) Math.hypot(mapa.centroX(c) - salida[0], mapa.centroZ(f) - salida[1]);
                if ((norteSur || esteOeste) && distancia > 15) {
                    candidatas.add(new int[] { f, c, norteSur ? 0 : 1 });
                }
            }
        }
        for (int i = 0; i < cantidad && !candidatas.isEmpty(); i++) {
            int[] celda = candidatas.get((i * candidatas.size() / cantidad + candidatas.size() / (2 * cantidad))
                    % candidatas.size());
            int direccion = celda[2] + (i % 2 == 0 ? 0 : 2); // alterna sentidos: N/S o E/O
            vehiculos.add(new VehiculoAutonomo(mapa, celda[0], celda[1], direccion, 1234L + i * 7919L,
                    COLORES[i % COLORES.length]));
        }
        espera = new float[vehiculos.size()];
        ignorar = new float[vehiculos.size()];
    }

    public void reiniciar() {
        for (int i = 0; i < vehiculos.size(); i++) {
            vehiculos.get(i).reiniciar();
            espera[i] = 0;
            ignorar[i] = 0;
        }
    }

    public void actualizar(float deltaTime, Semaforos semaforos, Auto jugador) {
        actualizar(deltaTime, semaforos, jugador, new float[0]);
    }

    /**
     * @param obstaculosJugador puntos {x0, z0, x1, z1, ...} que tambien ocupa el jugador
     *                          (los circulos de la chata): los autos autonomos frenan ante ellos
     */
    public void actualizar(float deltaTime, Semaforos semaforos, Auto jugador, float[] obstaculosJugador) {
        for (int i = 0; i < vehiculos.size(); i++) {
            VehiculoAutonomo v = vehiculos.get(i);
            float limite = limitePorSemaforo(v, semaforos);
            float porJugador = limitePorVehiculoDelante(v, jugador.x, jugador.z);
            for (int k = 0; k + 1 < obstaculosJugador.length; k += 2) {
                porJugador = Math.min(porJugador, limitePorVehiculoDelante(v, obstaculosJugador[k], obstaculosJugador[k + 1]));
            }
            float porTrafico = Float.MAX_VALUE;
            ignorar[i] = Math.max(0, ignorar[i] - deltaTime);
            if (ignorar[i] == 0) {
                for (VehiculoAutonomo otro : vehiculos) {
                    if (otro != v) {
                        porTrafico = Math.min(porTrafico, limitePorVehiculoDelante(v, otro.x, otro.z));
                    }
                }
            }
            // Bloqueo entre autos autonomos (por ejemplo en un cruce sin semaforo)
            if (porTrafico < 0.1f && v.velocidad < 0.1f) {
                espera[i] += deltaTime;
                if (espera[i] > 3) {
                    ignorar[i] = 1.5f;
                    espera[i] = 0;
                }
            } else {
                espera[i] = 0;
            }
            v.actualizar(deltaTime, Math.min(limite, Math.min(porJugador, porTrafico)));
        }
    }

    /** Velocidad maxima para detenerse en la linea de pare si el semaforo no esta en verde. */
    private float limitePorSemaforo(VehiculoAutonomo v, Semaforos semaforos) {
        int fs = v.filaSiguiente();
        int cs = v.columnaSiguiente();
        if (!mapa.tieneSemaforo(fs, cs) || mapa.tieneSemaforo(v.fila(), v.columna())) {
            return Float.MAX_VALUE;
        }
        boolean ejeNorteSur = v.direccionSalida() % 2 == 0;
        Semaforos.Luz luz = semaforos.estado(fs, cs, ejeNorteSur);
        float hastaLinea = v.restanteEnCelda() - DISTANCIA_LINEA_PARE;
        if (luz == Semaforos.Luz.VERDE || hastaLinea < -0.5f) {
            return Float.MAX_VALUE; // verde, o ya paso la linea: sigue
        }
        if (luz == Semaforos.Luz.AMARILLO && hastaLinea < 2.5f) {
            return Float.MAX_VALUE; // amarillo demasiado cerca para frenar: cruza
        }
        // v^2 = 2 a d: velocidad con la que todavia se puede frenar antes de la linea
        return (float) Math.sqrt(2 * FRENADO_SUAVE * Math.max(0, hastaLinea));
    }

    /** Mantiene unas 4 unidades de separacion con quien va delante en el mismo carril. */
    private float limitePorVehiculoDelante(VehiculoAutonomo v, float ox, float oz) {
        float dx = ox - v.x;
        float dz = oz - v.z;
        float fx = v.frenteX();
        float fz = v.frenteZ();
        float adelante = dx * fx + dz * fz;
        float lateral = Math.abs(dx * fz - dz * fx);
        if (adelante > 0 && adelante < 9 && lateral < 2.2f) {
            return Math.max(0, adelante - 4.2f) * 1.8f;
        }
        return Float.MAX_VALUE;
    }

    /** Colision del jugador con el trafico: no puede acercarse mas a un vehiculo que ya toca. */
    public boolean libre(float xAntes, float zAntes, float x, float z) {
        return libre(xAntes, zAntes, x, z, RADIO);
    }

    /** Igual que libre(), para un circulo de otro radio (por ejemplo, la chata). */
    public boolean libre(float xAntes, float zAntes, float x, float z, float radio) {
        float minimo = (RADIO + radio) * 0.9f;
        for (VehiculoAutonomo v : vehiculos) {
            float ahora = (float) Math.hypot(x - v.x, z - v.z);
            float antes = (float) Math.hypot(xAntes - v.x, zAntes - v.z);
            if (ahora < minimo && ahora < antes) {
                return false;
            }
        }
        return true;
    }

    public void dibujar(Renderizador r, boolean noche) {
        for (VehiculoAutonomo v : vehiculos) {
            ModeloAuto.dibujar(r, v.x, v.z, v.angulo, v.color, noche, noche);
        }
    }

    /** Cuadrados de color en el minimapa (con una marca hacia su frente). */
    public void dibujarEnMapa(Renderizador r, float escala) {
        float e = escala * 1.7f;
        for (VehiculoAutonomo v : vehiculos) {
            r.cajaGirada(v.x, 35, v.z, 2.2f * e, 0.1f, 3.2f * e, v.color[0], v.color[1], v.color[2], v.angulo);
        }
    }
}
