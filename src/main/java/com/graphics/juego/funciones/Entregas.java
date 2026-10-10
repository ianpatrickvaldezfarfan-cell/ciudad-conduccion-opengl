package com.graphics.juego.funciones;

import java.util.List;

import com.graphics.juego.ciudad.MapaCiudad;
import com.graphics.juego.conduccion.Auto;
import com.graphics.juego.motor.Renderizador;

/**
 * Juego de entregas: los destinos salen de los digitos '1'..'9' del mapa.
 * Una entrega se completa al llegar al destino activo y frenar.
 */
public class Entregas {

    public static final float RADIO_LLEGADA = 3.5f;
    public static final float VELOCIDAD_LLEGADA = 1.5f;

    private static final String[] NOMBRES = {
            "PANADERIA", "HOSPITAL", "ESCUELA", "BIBLIOTECA", "ESTACION", "MERCADO", "MUSEO", "ESTADIO", "PUERTO"
    };

    private final List<float[]> destinos;
    private int completadas;
    private float tiempo;
    /** Segundos que queda visible el aviso de "entrega completada". */
    private float aviso;
    /** Reloj de animacion (baliza): avanza con deltaTime. */
    private float animacion;

    public Entregas(MapaCiudad mapa) {
        destinos = mapa.destinos();
        if (destinos.isEmpty()) {
            throw new IllegalStateException("El mapa no tiene destinos '1'..'9'");
        }
    }

    public void reiniciar() {
        completadas = 0;
        tiempo = 0;
        aviso = 0;
    }

    public void actualizar(float deltaTime, Auto auto) {
        animacion += deltaTime;
        aviso = Math.max(0, aviso - deltaTime);
        if (terminado()) {
            return;
        }
        tiempo += deltaTime;
        float[] d = destinos.get(completadas);
        float dx = auto.x - d[0];
        float dz = auto.z - d[1];
        boolean cerca = dx * dx + dz * dz < RADIO_LLEGADA * RADIO_LLEGADA;
        if (cerca && Math.abs(auto.velocidad) < VELOCIDAD_LLEGADA) {
            completadas++;
            aviso = 2.5f;
        }
    }

    public boolean terminado() {
        return completadas >= destinos.size();
    }

    public int completadas() {
        return completadas;
    }

    public int total() {
        return destinos.size();
    }

    public float tiempo() {
        return tiempo;
    }

    /** Destino activo {x, z} o null si ya se completaron todos. */
    public float[] activo() {
        return terminado() ? null : destinos.get(completadas);
    }

    public String nombreActivo() {
        return terminado() ? "" : NOMBRES[completadas % NOMBRES.length];
    }

    public boolean mostrandoAviso() {
        return aviso > 0;
    }

    /** Rumbo aproximado desde el auto hacia el destino activo: N, NE, E, SE, S, SO, O, NO. */
    public String rumbo(Auto auto) {
        float[] d = activo();
        if (d == null) {
            return "";
        }
        // 0 grados = norte (-Z), 90 = este (+X)
        double grados = Math.toDegrees(Math.atan2(d[0] - auto.x, -(d[1] - auto.z)));
        String[] nombres = { "N", "NE", "E", "SE", "S", "SO", "O", "NO" };
        int sector = (int) Math.floorMod(Math.round(grados / 45.0), 8);
        return nombres[sector];
    }

    public List<float[]> destinos() {
        return destinos;
    }

    /** Distancia desde el auto al destino activo (0 si no hay). */
    public float distancia(Auto auto) {
        float[] d = activo();
        if (d == null) {
            return 0;
        }
        return (float) Math.hypot(auto.x - d[0], auto.z - d[1]);
    }

    /** Marca dorada sobre el asfalto, columna de luz y baliza giratoria. */
    public void dibujar(Renderizador r) {
        float[] d = activo();
        if (d == null) {
            return;
        }
        float pulso = 0.75f + 0.25f * (float) Math.sin(animacion * 4);
        r.emision(true);
        r.caja(d[0], 0.04f, d[1], RADIO_LLEGADA * 2, 0.05f, RADIO_LLEGADA * 2, 1 * pulso, 0.72f * pulso, 0.12f);
        r.caja(d[0], 0.06f, d[1], RADIO_LLEGADA * 2 - 1.2f, 0.05f, RADIO_LLEGADA * 2 - 1.2f, 0.25f, 0.18f, 0.05f);
        float altura = 4 + (float) Math.sin(animacion * 2) * 0.35f;
        r.cajaGirada(d[0], altura, d[1], 0.9f, 0.9f, 0.9f, 1, 0.8f, 0.15f, animacion);
        r.caja(d[0], 12, d[1], 0.25f, 16, 0.25f, 1, 0.85f * pulso, 0.3f); // columna visible de lejos
        r.emision(false);
    }

    /** Version plana para el minimapa. */
    public void dibujarEnMapa(Renderizador r) {
        float[] d = activo();
        if (d == null) {
            return;
        }
        float pulso = 0.7f + 0.3f * (float) Math.sin(animacion * 5);
        r.caja(d[0], 30, d[1], 6.5f * pulso + 2, 0.1f, 6.5f * pulso + 2, 1, 0.75f, 0.1f);
    }
}
