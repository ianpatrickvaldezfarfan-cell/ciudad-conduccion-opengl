package com.graphics.juego.conduccion;

import com.graphics.juego.ciudad.MapaCiudad;
import com.graphics.juego.motor.Matriz;

/**
 * Camaras de la vista principal (tecla C las recorre):
 *  - TERCERA PERSONA: detras del auto, siguiendo su orientacion.
 *  - AEREA: alta sobre el auto, para ver los cruces cercanos.
 *  - PANORAMICA: toda la ciudad; su distancia sale del tamano del mapa (Q/E la giran).
 * La camara se acerca a su posicion ideal con un suavizado exponencial que depende
 * de deltaTime, asi el movimiento es igual con cualquier cantidad de FPS.
 */
public class Camara {

    public enum Modo {
        TERCERA_PERSONA("TERCERA PERSONA"), AEREA("AEREA"), PANORAMICA("PANORAMICA");

        public final String nombre;

        Modo(String nombre) {
            this.nombre = nombre;
        }
    }

    private Modo modo = Modo.TERCERA_PERSONA;
    private final float[] ojo = new float[3];
    private final float[] objetivo = new float[3];
    private boolean primerCuadro = true;
    private float orbita = 0.7f;

    public Modo modo() {
        return modo;
    }

    public void siguienteModo() {
        modo = Modo.values()[(modo.ordinal() + 1) % Modo.values().length];
    }

    /** Coloca la camara en su sitio sin transicion (al iniciar o reiniciar). */
    public void saltar() {
        primerCuadro = true;
    }

    public float[] ojo() {
        return ojo;
    }

    /**
     * @param giroOrbita -1, 0 o +1 segun Q/E (solo afecta a la panoramica)
     * @param conChata   con la chata enganchada la tercera persona se aleja y sube
     *                   para que el auto y la chata se vean completos
     */
    public void actualizar(float deltaTime, Auto auto, MapaCiudad mapa, float giroOrbita, boolean conChata) {
        orbita += giroOrbita * 0.8f * deltaTime;
        float distancia = conChata ? 13.5f : 11;
        float altura = conChata ? 7.0f : 5.5f;
        float[] ideal = new float[6]; // ojo XYZ y objetivo XYZ
        switch (modo) {
            case TERCERA_PERSONA -> {
                ideal[0] = auto.x - auto.frenteX() * distancia;
                ideal[1] = altura;
                ideal[2] = auto.z - auto.frenteZ() * distancia;
                ideal[3] = auto.x + auto.frenteX() * 4;
                ideal[4] = 1.0f;
                ideal[5] = auto.z + auto.frenteZ() * 4;
            }
            case AEREA -> {
                ideal[0] = auto.x - auto.frenteX() * 10;
                ideal[1] = 34;
                ideal[2] = auto.z - auto.frenteZ() * 10 + 0.01f;
                ideal[3] = auto.x + auto.frenteX() * 3;
                ideal[4] = 0;
                ideal[5] = auto.z + auto.frenteZ() * 3;
            }
            case PANORAMICA -> {
                float radio = mapa.limite() * 1.85f;
                ideal[0] = (float) Math.sin(orbita) * radio;
                ideal[1] = mapa.limite() * 1.5f;
                ideal[2] = (float) Math.cos(orbita) * radio;
                ideal[3] = 0;
                ideal[4] = 0;
                ideal[5] = 0;
            }
        }
        // Suavizado: fraccion del camino recorrida en este cuadro = 1 - e^(-k * dt)
        float t = primerCuadro ? 1 : 1 - (float) Math.exp(-6 * deltaTime);
        primerCuadro = false;
        for (int i = 0; i < 3; i++) {
            ojo[i] += (ideal[i] - ojo[i]) * t;
            objetivo[i] += (ideal[i + 3] - objetivo[i]) * t;
        }
    }

    /** Matriz proyeccion * vista para la ventana con la proporcion dada. */
    public float[] vistaProyeccion(float aspecto, MapaCiudad mapa) {
        float lejos = mapa.limite() * 6;
        float[] proyeccion = Matriz.perspectiva(55, aspecto, 0.3f, lejos);
        float[] vista = Matriz.mirarA(ojo[0], ojo[1], ojo[2], objetivo[0], objetivo[1], objetivo[2], 0, 1, 0);
        return Matriz.multiplicar(proyeccion, vista);
    }
}
