package com.graphics.juego.conduccion;

/**
 * Estado y fisica del auto del jugador. No usa OpenGL: se puede probar sin ventana.
 *
 * Convencion de angulo: 0 apunta hacia el norte (-Z); el frente es (-sin a, -cos a).
 * Todo se integra con deltaTime (segundos), por eso el manejo es igual a 30 o 144 FPS.
 */
public class Auto {

    /** Radio del circulo de colisiones: contiene toda la carroceria. */
    public static final float RADIO = 1.65f;

    public static final float ACELERACION = 9;      // unidades / s^2
    public static final float VELOCIDAD_MAXIMA = 17; // unidades / s (1 unidad = 1 m)
    public static final float REVERSA_MAXIMA = -6;
    private static final float RESISTENCIA = 0.7f;   // perdida de velocidad al rodar
    private static final float RESISTENCIA_FRENO = 7;
    private static final float GIRO = 0.11f;         // radianes por unidad recorrida

    private final float xInicial;
    private final float zInicial;
    private final float anguloInicial;

    public float x;
    public float z;
    public float angulo;
    public float velocidad;
    /** true durante el cuadro en que el auto choco contra algo. */
    private boolean choco;

    public Auto(float x, float z, float angulo) {
        this.xInicial = x;
        this.zInicial = z;
        this.anguloInicial = angulo;
        reiniciar();
    }

    /** Restablece posicion, orientacion y velocidad (tecla R). */
    public void reiniciar() {
        x = xInicial;
        z = zInicial;
        angulo = anguloInicial;
        velocidad = 0;
        choco = false;
    }

    public float frenteX() {
        return -(float) Math.sin(angulo);
    }

    public float frenteZ() {
        return -(float) Math.cos(angulo);
    }

    public int kmh() {
        return Math.round(Math.abs(velocidad) * 3.6f);
    }

    public boolean choco() {
        return choco;
    }

    /** Avanza la simulacion deltaTime segundos. */
    public void actualizar(float deltaTime, ControlesAuto controles, Obstaculos obstaculos) {
        // 1) Acelerador: integra la aceleracion en el tiempo
        velocidad += controles.acelerador() * ACELERACION * deltaTime;

        // 2) Resistencia exponencial: la fraccion conservada depende solo del tiempo
        float resistencia = controles.freno() ? RESISTENCIA_FRENO : RESISTENCIA;
        velocidad *= (float) Math.exp(-resistencia * deltaTime);
        velocidad = Math.max(REVERSA_MAXIMA, Math.min(VELOCIDAD_MAXIMA, velocidad));
        if (controles.acelerador() == 0 && Math.abs(velocidad) < 0.05f) {
            velocidad = 0;
        }

        // 3) Giro proporcional a la distancia recorrida (en reversa se invierte)
        angulo += controles.direccion() * velocidad * GIRO * deltaTime;

        // 4) Movimiento con colisiones: si choca de frente intenta deslizarse por la pared
        float dx = frenteX() * velocidad * deltaTime;
        float dz = frenteZ() * velocidad * deltaTime;
        choco = false;
        if (obstaculos.libre(x, z, x + dx, z + dz)) {
            x += dx;
            z += dz;
        } else {
            choco = true;
            if (Math.abs(dx) > Math.abs(dz) && obstaculos.libre(x, z, x + dx, z)) {
                x += dx;
            } else if (obstaculos.libre(x, z, x, z + dz)) {
                z += dz;
            } else if (obstaculos.libre(x, z, x + dx, z)) {
                x += dx;
            } else {
                velocidad = 0; // bloqueado por completo: se detiene
            }
            // Rozar la pared frena el auto (tambien dependiente del tiempo)
            velocidad *= (float) Math.exp(-3 * deltaTime);
        }
    }
}
