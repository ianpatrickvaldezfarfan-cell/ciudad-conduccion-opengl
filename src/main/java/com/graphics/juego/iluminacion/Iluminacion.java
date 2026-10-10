package com.graphics.juego.iluminacion;

import java.util.List;

import com.graphics.juego.ciudad.MapaCiudad;
import com.graphics.juego.conduccion.Auto;
import com.graphics.juego.conduccion.ModeloAuto;
import com.graphics.juego.motor.Renderizador;

/**
 * Iluminacion de la escena:
 *  - Dia / noche (tecla N): luz ambiental + luz direccional (sol o luna).
 *  - Farolas: luces puntuales cuya posicion sale del mapa; de noche iluminan el suelo
 *    y los edificios cercanos y su aporte disminuye con la distancia.
 *  - Faros (tecla F): dos focos que siguen la posicion y orientacion del auto,
 *    con un cono hacia delante y alcance limitado.
 *
 * La bombilla de cada farola se dibuja con emision (color propio, siempre brillante),
 * pero eso NO ilumina nada: la luz del entorno la calcula el shader con uFarolas.
 */
public class Iluminacion {

    public static final float ALCANCE_FAROLA = 20;
    public static final float ALCANCE_FAROS = 30;

    private final List<float[]> farolas;
    private boolean noche = false;
    private boolean faros = true;

    public Iluminacion(MapaCiudad mapa) {
        farolas = mapa.farolas();
        if (farolas.size() > Renderizador.MAX_FAROLAS) {
            throw new IllegalStateException("El shader admite como maximo " + Renderizador.MAX_FAROLAS + " farolas");
        }
    }

    public boolean noche() {
        return noche;
    }

    public boolean faros() {
        return faros;
    }

    public void alternarNoche() {
        noche = !noche;
    }

    public void alternarFaros() {
        faros = !faros;
    }

    public int cantidadFarolas() {
        return farolas.size();
    }

    public float[] colorCielo() {
        return noche ? new float[] { 0.02f, 0.03f, 0.07f } : new float[] { 0.55f, 0.74f, 0.92f };
    }

    /** Envia al shader todas las luces del cuadro actual. */
    public void aplicar(Renderizador r, float[] camara, float alcanceNiebla, Auto auto) {
        // 1) Ambiente y luz direccional
        if (noche) {
            r.vector("uAmbiente", 0.075f, 0.09f, 0.15f);   // azul tenue: se distinguen las formas
            r.vector("uSolDir", -0.30f, 0.85f, 0.43f);     // luna
            r.vector("uSolColor", 0.12f, 0.14f, 0.24f);
        } else {
            r.vector("uAmbiente", 0.40f, 0.42f, 0.47f);
            r.vector("uSolDir", 0.37f, 0.84f, 0.40f);      // sol alto desde el sureste
            r.vector("uSolColor", 0.75f, 0.70f, 0.60f);
        }

        // 2) Farolas: solo encendidas de noche
        r.entero("uNumFarolas", noche ? farolas.size() : 0);
        r.vector("uFarolaColor", 1.0f, 0.74f, 0.38f);
        r.decimal("uFarolaAlcance", ALCANCE_FAROLA);
        for (int i = 0; i < farolas.size(); i++) {
            float[] f = farolas.get(i);
            r.vector("uFarolas[" + i + "]", f[2], f[3], f[4]);
        }

        // 3) Faros: posicion de cada foco y direccion = frente del auto inclinado al suelo
        r.entero("uFaros", faros ? 1 : 0);
        r.decimal("uFaroAlcance", ALCANCE_FAROS);
        r.decimal("uFaroIntensidad", noche ? 7.0f : 2.0f);
        for (int lado = 0; lado < 2; lado++) {
            float[] p = ModeloAuto.posicionFaro(auto.x, auto.z, auto.angulo, lado == 0 ? -1 : 1);
            r.vector("uFaroPos[" + lado + "]", p[0], p[1], p[2]);
        }
        float inclinacion = -0.10f;
        float largo = (float) Math.sqrt(1 + inclinacion * inclinacion);
        r.vector("uFaroDir", auto.frenteX() / largo, inclinacion / largo, auto.frenteZ() / largo);

        // 4) Niebla hacia el color del cielo
        float[] cielo = colorCielo();
        r.vector("uNiebla", cielo[0], cielo[1], cielo[2]);
        r.vector("uCamara", camara[0], camara[1], camara[2]);
        r.decimal("uNieblaInicio", alcanceNiebla * 0.45f);
        r.decimal("uNieblaFin", alcanceNiebla);
    }

    /** Postes, brazos y bombillas en las mismas posiciones que se envian al shader. */
    public void dibujarFarolas(Renderizador r) {
        for (float[] f : farolas) {
            float px = f[0];
            float pz = f[1];
            r.caja(px, 0.3f + 2.75f, pz, 0.2f, 5.5f, 0.2f, 0.22f, 0.24f, 0.27f);   // poste
            float bx = f[2];
            float bz = f[4];
            float mx = (px + bx) / 2;
            float mz = (pz + bz) / 2;
            r.cajaGirada(mx, f[3] + 0.32f, mz, 0.12f, 0.12f, 1.65f, 0.22f, 0.24f, 0.27f,
                    (float) Math.atan2(bx - px, bz - pz));                             // brazo
            r.caja(bx, f[3] + 0.3f, bz, 0.8f, 0.14f, 0.8f, 0.18f, 0.19f, 0.21f);      // pantalla
            r.emision(noche);
            if (noche) {
                r.caja(bx, f[3], bz, 0.55f, 0.32f, 0.55f, 1.0f, 0.88f, 0.55f);       // bombilla encendida
            } else {
                r.caja(bx, f[3], bz, 0.55f, 0.32f, 0.55f, 0.75f, 0.75f, 0.70f);      // bombilla apagada
            }
            r.emision(false);
        }
    }
}
