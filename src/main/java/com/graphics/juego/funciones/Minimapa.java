package com.graphics.juego.funciones;

import static org.lwjgl.opengl.GL33.*;

import com.graphics.juego.ciudad.MapaCiudad;
import com.graphics.juego.motor.Matriz;
import com.graphics.juego.motor.Renderizador;

/**
 * Minimapa: vista superior simultanea en la esquina superior derecha (tecla M).
 *
 *  - Tiene su propio glViewport y su propia proyeccion ORTOGRAFICA, con el norte (-Z) arriba.
 *  - glScissor limita el borrado a su recuadro: la vista principal no se altera.
 *  - Su tamano se recalcula cada cuadro con el tamano del framebuffer (que llega por el
 *    framebuffer size callback), asi conserva el encuadre al redimensionar la ventana.
 *  - Su escala sale del tamano del mapa: siempre se ve la ciudad completa.
 *  - Al terminar restaura viewport, scissor, profundidad y emision.
 */
public class Minimapa {

    private final MapaCiudad mapa;
    private boolean visible = true;

    public Minimapa(MapaCiudad mapa) {
        this.mapa = mapa;
    }

    public boolean visible() {
        return visible;
    }

    public void alternar() {
        visible = !visible;
    }

    /** Recuadro en pixeles {x, y, lado} (y medido desde abajo, como OpenGL). */
    public int[] recuadro(int ancho, int alto) {
        int menor = Math.min(ancho, alto);
        int lado = Math.max(110, Math.min(320, Math.round(menor * 0.30f)));
        lado = Math.min(lado, menor - 20); // ventanas muy pequenas
        int margen = Math.max(10, menor / 50);
        return new int[] { ancho - lado - margen, alto - lado - margen, lado };
    }

    /** Lo que se dibuja encima de la ciudad (auto, destino, trafico) con la proyeccion del mapa. */
    public interface Contenido {
        void dibujar(Renderizador r, float escalaIndicador);
    }

    public void dibujar(Renderizador r, int ancho, int alto, Contenido contenido) {
        if (!visible || ancho < 60 || alto < 60) {
            return;
        }
        int[] q = recuadro(ancho, alto);
        int x = q[0];
        int y = q[1];
        int lado = q[2];

        glEnable(GL_SCISSOR_TEST);
        try {
            // Marco claro de 3 px y fondo, borrando solo dentro del recuadro
            glScissor(x - 3, y - 3, lado + 6, lado + 6);
            glClearColor(0.85f, 0.88f, 0.92f, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glScissor(x, y, lado, lado);
            glClearColor(0.10f, 0.13f, 0.17f, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glViewport(x, y, lado, lado);
            float extension = mapa.limite() * 1.04f;
            float[] proyeccion = Matriz.ortografica(-extension, extension, -extension, extension, 1, 200);
            // Camara cenital: arriba de la pantalla = -Z (norte)
            float[] vista = Matriz.mirarA(0, 100, 0, 0, 0, 0, 0, 0, -1);
            r.vistaProyeccion(Matriz.multiplicar(proyeccion, vista));
            r.emision(true); // colores planos, legibles de dia y de noche
            r.niebla(1e6f, 2e6f); // sin niebla (la vista principal la vuelve a fijar cada cuadro)

            dibujarCiudad(r);
            contenido.dibujar(r, mapa.limite() / 55f);
        } finally {
            r.emision(false);
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, ancho, alto);
        }
    }

    /** Version simplificada de la ciudad, generada desde la misma matriz. */
    private void dibujarCiudad(Renderizador r) {
        float lx = mapa.limiteX();
        float lz = mapa.limiteZ();
        r.caja(0, 0, 0, lx * 2, 0.1f, lz * 2, 0.24f, 0.26f, 0.30f); // calles
        float celda = MapaCiudad.CELDA;
        for (int f = 0; f < mapa.filas(); f++) {
            for (int c = 0; c < mapa.columnas(); c++) {
                if (mapa.esCalle(f, c)) {
                    continue;
                }
                float x = mapa.centroX(c);
                float z = mapa.centroZ(f);
                r.caja(x, 1, z, celda - 0.6f, 0.1f, celda - 0.6f, 0.58f, 0.60f, 0.62f); // acera
                if (mapa.esEdificio(f, c)) {
                    MapaCiudad.Edificio e = mapa.edificio(f, c);
                    r.caja(x, 2, z, 7, 0.1f, 7, e.r(), e.g(), e.b());
                } else {
                    r.caja(x, 2, z, 8.4f, 0.1f, 8.4f, 0.22f, 0.58f, 0.30f);
                }
            }
        }
    }
}
