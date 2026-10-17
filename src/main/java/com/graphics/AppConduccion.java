package com.graphics;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import com.graphics.juego.ciudad.DibujoCiudad;
import com.graphics.juego.ciudad.Letreros;
import com.graphics.juego.ciudad.MapaCiudad;
import com.graphics.juego.conduccion.Auto;
import com.graphics.juego.conduccion.Chata;
import com.graphics.juego.conduccion.Camara;
import com.graphics.juego.conduccion.ControlesAuto;
import com.graphics.juego.conduccion.ModeloAuto;
import com.graphics.juego.conduccion.ModeloChata;
import com.graphics.juego.funciones.Entregas;
import com.graphics.juego.funciones.Hud;
import com.graphics.juego.funciones.Minimapa;
import com.graphics.juego.funciones.PanelHud;
import com.graphics.juego.funciones.Semaforos;
import com.graphics.juego.iluminacion.Iluminacion;
import com.graphics.juego.motor.Renderizador;
import com.graphics.juego.trafico.Trafico;

/**
 * Ciudad interactiva: conducir un auto en tercera persona por una ciudad data-driven,
 * completar entregas, con dia/noche, farolas, faros, semaforos, minimapa, trafico y HUD.
 *
 * Esta clase solo organiza el programa (ventana, teclado, ciclo y orden de dibujo).
 * Cada responsabilidad vive en su propio paquete dentro de com.graphics.juego:
 *   motor        -> shaders, cubo, matrices, fuente de pixeles
 *   ciudad       -> mapa (matriz de celdas), dibujo de la ciudad y decoracion, letreros
 *   conduccion   -> auto, chata (remolque), fisica, colisiones, modelos 3D, camara
 *   iluminacion  -> dia/noche, farolas, faros
 *   funciones    -> entregas, semaforos, minimapa, HUD
 *   trafico      -> vehiculos autonomos
 *
 * Opciones de prueba (se pasan a la JVM con -D):
 *   -Ddemo.frames=N        cierra la ventana despues de N cuadros
 *   -Ddemo.captura=a.png   guarda una captura del ultimo cuadro
 *   -Ddemo.noche=true      inicia de noche
 *   -Ddemo.camara=N        0 tercera persona, 1 aerea, 2 panoramica
 *   -Ddemo.auto=x,z,ang    coloca el auto en otra posicion
 *   -Ddemo.ayuda=false     inicia con la ayuda oculta
 *   -Ddemo.pausa=true      inicia en pausa
 *   -Ddemo.ganar=true      completa todas las entregas al iniciar (pantalla de victoria)
 *   -Ddemo.redimensionar=WxH  cambia el tamano de la ventana en el cuadro 30
 *   -Ddemo.chataAngulo=rad abre la chata ese angulo respecto al auto (para revisar el modelo)
 */
public class AppConduccion {

    private static final String TITULO = "Ciudad Conduccion";
    private static final float[] COLOR_JUGADOR = { 0.92f, 0.20f, 0.12f };
    private static final int VEHICULOS_AUTONOMOS = 5;

    private long ventana;
    private int ancho = 1280;
    private int alto = 800;
    private boolean listo; // OpenGL ya inicializado (para dibujar durante el redimensionado)

    // Estado del juego
    private final MapaCiudad mapa = new MapaCiudad();
    private final Auto auto;
    private final Chata chata;
    private final Camara camara = new Camara();
    private final Entregas entregas = new Entregas(mapa);
    private final Semaforos semaforos = new Semaforos(mapa);
    private final Trafico trafico = new Trafico(mapa, VEHICULOS_AUTONOMOS);
    private final Iluminacion iluminacion = new Iluminacion(mapa);
    private boolean pausado;
    private float avisoSinEspacio; // segundos que se muestra "no hay espacio para la chata"

    // Dibujo
    private final Renderizador renderizador = new Renderizador();
    private final DibujoCiudad dibujoCiudad = new DibujoCiudad(mapa);
    private final Letreros letreros = new Letreros(mapa);
    private final Minimapa minimapa = new Minimapa(mapa);
    private final Hud hud = new Hud();
    private final PanelHud panel = new PanelHud();

    private float relojTitulo;

    public AppConduccion() {
        float[] salida = mapa.salida();
        auto = new Auto(salida[0], salida[1], salida[2]);
        chata = new Chata(auto); // por defecto enganchada y alineada detras del auto
    }

    public void run() {
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err);
        errores.set();
        try {
            iniciar();
            loop();
        } finally {
            if (listo) {
                hud.destruir();
                renderizador.destruir();
            }
            if (ventana != 0) {
                Callbacks.glfwFreeCallbacks(ventana);
                glfwDestroyWindow(ventana);
            }
            glfwTerminate();
            glfwSetErrorCallback(null);
            errores.free();
        }
    }

    // ===================== Inicio =====================

    private void iniciar() {
        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        ventana = glfwCreateWindow(ancho, alto, TITULO, 0, 0);
        if (ventana == 0) {
            throw new IllegalStateException("No se pudo crear la ventana OpenGL 3.3");
        }
        glfwMakeContextCurrent(ventana);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);

        // Tamano real en pixeles del framebuffer
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(ventana, w, h);
        ancho = w[0];
        alto = h[0];

        // Framebuffer size callback: se llama cada vez que cambia el tamano de la ventana.
        // Guarda el nuevo tamano (lo usan la proyeccion, el minimapa y el HUD) y redibuja,
        // porque en Windows el ciclo principal se detiene mientras se arrastra el borde.
        glfwSetFramebufferSizeCallback(ventana, (v, nuevoAncho, nuevoAlto) -> {
            ancho = nuevoAncho;
            alto = nuevoAlto;
            if (listo && ancho > 0 && alto > 0) {
                dibujarFrame();
                glfwSwapBuffers(v);
            }
        });

        glfwSetKeyCallback(ventana, (v, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                tecla(key);
            }
        });

        renderizador.crear();
        hud.crear();
        listo = true;
        aplicarOpcionesDemo();
        glfwShowWindow(ventana);
    }

    private void aplicarOpcionesDemo() {
        if (Boolean.getBoolean("demo.noche")) {
            iluminacion.alternarNoche();
        }
        for (int i = 0; i < Integer.getInteger("demo.camara", 0); i++) {
            camara.siguienteModo();
        }
        if ("false".equals(System.getProperty("demo.ayuda"))) {
            panel.alternarAyuda();
        }
        pausado = Boolean.getBoolean("demo.pausa");
        if (Boolean.getBoolean("demo.ganar")) {
            for (float[] d : mapa.destinos()) {
                auto.x = d[0];
                auto.z = d[1];
                entregas.actualizar(0.01f, auto);
            }
            auto.reiniciar();
        }
        String posicion = System.getProperty("demo.auto");
        if (posicion != null) {
            String[] partes = posicion.split(",");
            auto.x = Float.parseFloat(partes[0]);
            auto.z = Float.parseFloat(partes[1]);
            auto.angulo = Float.parseFloat(partes[2]);
        }
        chata.alinear(auto);
        String abrir = System.getProperty("demo.chataAngulo");
        if (abrir != null) {
            float[] p = Chata.enganche(auto.x, auto.z, auto.angulo);
            chata.angulo = auto.angulo - Float.parseFloat(abrir);
            chata.ejeX = p[0] + (float) Math.sin(chata.angulo) * Chata.LANZA;
            chata.ejeZ = p[1] + (float) Math.cos(chata.angulo) * Chata.LANZA;
        }
    }

    // ===================== Teclado =====================

    /** Acciones de una sola pulsacion. */
    private void tecla(int key) {
        switch (key) {
            case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(ventana, true);
            case GLFW_KEY_C -> camara.siguienteModo();
            case GLFW_KEY_N -> iluminacion.alternarNoche();
            case GLFW_KEY_F -> iluminacion.alternarFaros();
            case GLFW_KEY_M -> minimapa.alternar();
            case GLFW_KEY_R -> reiniciar();
            case GLFW_KEY_H -> panel.alternarAyuda();
            case GLFW_KEY_P -> pausado = !pausado;
            case GLFW_KEY_T -> alternarChata();
            default -> {
            }
        }
    }

    /** Tecla T: suelta la chata o la engancha alineada detras del auto (si hay espacio). */
    private void alternarChata() {
        if (chata.enganchada()) {
            chata.desenganchar();
            return;
        }
        chata.enganchar(auto);
        if (!Chata.cabe(chata.estado(), chata.estado(), this::libreChata)) {
            chata.desenganchar(); // quedaria dentro de una manzana o de otro auto
            avisoSinEspacio = 2.5f;
        }
    }

    private boolean pulsada(int key) {
        return glfwGetKey(ventana, key) == GLFW_PRESS;
    }

    /** Lee las teclas que se mantienen presionadas y las convierte en controles del auto. */
    private ControlesAuto leerControles() {
        float acelerador = 0;
        float direccion = 0;
        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) {
            acelerador += 1;
        }
        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) {
            acelerador -= 1;
        }
        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) {
            direccion += 1;
        }
        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) {
            direccion -= 1;
        }
        return new ControlesAuto(acelerador, direccion, pulsada(GLFW_KEY_SPACE));
    }

    /**
     * Tecla R: el auto vuelve a la salida con su orientacion inicial y velocidad cero,
     * las entregas y el tiempo vuelven a cero y el trafico a su posicion inicial:
     * se puede completar de nuevo toda la actividad.
     */
    private void reiniciar() {
        auto.reiniciar();
        chata.alinear(auto); // vuelve recta detras del auto en la salida
        entregas.reiniciar();
        trafico.reiniciar();
        semaforos.reiniciar();
        camara.saltar();
        pausado = false;
    }

    // ===================== Ciclo principal =====================

    private void loop() {
        double anterior = glfwGetTime();
        int cuadros = 0;
        int maxCuadros = Integer.getInteger("demo.frames", 0);
        while (!glfwWindowShouldClose(ventana)) {
            glfwPollEvents();
            double ahora = glfwGetTime();
            // deltaTime: segundos desde el cuadro anterior, limitado a 50 ms para que
            // una pausa larga (mover la ventana) no haga atravesar paredes
            float deltaTime = (float) Math.min(ahora - anterior, 0.05);
            anterior = ahora;

            if (!pausado) {
                actualizar(deltaTime);
            }
            actualizarTitulo(deltaTime);
            if (ancho > 0 && alto > 0) {
                dibujarFrame();
            }
            cuadros++;
            redimensionarDemo(cuadros);
            if (maxCuadros > 0 && cuadros >= maxCuadros) {
                String captura = System.getProperty("demo.captura");
                if (captura != null && ancho > 0) {
                    guardarCaptura(captura);
                }
                glfwSetWindowShouldClose(ventana, true);
            }
            glfwSwapBuffers(ventana);
        }
    }

    /** Toda la simulacion avanza con deltaTime: nada depende de los FPS. */
    private void actualizar(float deltaTime) {
        // El auto arrastra a la chata: ningun movimiento se acepta si la chata chocaria
        chata.conducir(deltaTime, auto, leerControles(), this::libre, this::libreChata);
        avisoSinEspacio = Math.max(0, avisoSinEspacio - deltaTime);
        entregas.actualizar(deltaTime, auto);
        semaforos.actualizar(deltaTime);
        trafico.actualizar(deltaTime, semaforos, auto, chata.enganchada() ? chata.estado().circulos() : new float[0]);
        float orbita = (pulsada(GLFW_KEY_E) ? 1 : 0) - (pulsada(GLFW_KEY_Q) ? 1 : 0);
        camara.actualizar(deltaTime, auto, mapa, orbita, chata.enganchada());
    }

    /** Colisiones del jugador: manzanas y bordes del mapa, y vehiculos autonomos. */
    private boolean libre(float xAntes, float zAntes, float x, float z) {
        return mapa.puedeCircular(x, z, Auto.RADIO) && trafico.libre(xAntes, zAntes, x, z);
    }

    /** Colisiones de cada circulo de la chata: misma logica que el auto, con su radio. */
    private boolean libreChata(float xAntes, float zAntes, float x, float z) {
        return mapa.puedeCircular(x, z, Chata.RADIO) && trafico.libre(xAntes, zAntes, x, z, Chata.RADIO);
    }

    // ===================== Dibujo =====================

    private void dibujarFrame() {
        // 1) Vista principal en perspectiva, ocupando toda la ventana
        glViewport(0, 0, ancho, alto);
        float[] cielo = iluminacion.colorCielo();
        glClearColor(cielo[0], cielo[1], cielo[2], 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        renderizador.usar();
        renderizador.vistaProyeccion(camara.vistaProyeccion((float) ancho / alto, mapa));
        iluminacion.aplicar(renderizador, camara.ojo(), mapa.limite() * 5, auto);
        renderizador.emision(false);

        boolean noche = iluminacion.noche();
        dibujoCiudad.dibujar(renderizador, noche, true);
        letreros.dibujar(renderizador, noche);
        iluminacion.dibujarFarolas(renderizador);
        semaforos.dibujar(renderizador);
        entregas.dibujar(renderizador);
        trafico.dibujar(renderizador, noche);
        ModeloAuto.dibujar(renderizador, auto.x, auto.z, auto.angulo, COLOR_JUGADOR, noche, iluminacion.faros());
        if (chata.enganchada()) {
            ModeloChata.dibujarEnganche(renderizador, auto);
            ModeloChata.dibujar(renderizador, chata, noche);
        }

        // 2) Minimapa: su propio viewport y proyeccion ortografica (restaura el estado al terminar)
        minimapa.dibujar(renderizador, ancho, alto, (r, escala) -> {
            entregas.dibujarEnMapa(r);
            trafico.dibujarEnMapa(r, escala);
            dibujarFlechaJugador(r, escala);
        });

        // 3) HUD 2D encima de todo
        panel.dibujar(hud, ancho, alto, datosHud(), pausado);

        int error = glGetError();
        if (error != GL_NO_ERROR) {
            throw new IllegalStateException("Error OpenGL al dibujar: " + error);
        }
    }

    /** Flecha cian (posicion y orientacion del jugador) con punta blanca hacia su frente. */
    private void dibujarFlechaJugador(Renderizador r, float escala) {
        float e = escala * 2.2f;
        if (chata.enganchada()) {
            // Rectangulo naranja detras de la flecha, con el angulo propio de la chata
            float atrasX = auto.x - auto.frenteX() * 1.8f * e;
            float atrasZ = auto.z - auto.frenteZ() * 1.8f * e;
            float chataX = atrasX + (float) Math.sin(chata.angulo) * 1.4f * e;
            float chataZ = atrasZ + (float) Math.cos(chata.angulo) * 1.4f * e;
            r.cajaGirada(chataX, 39, chataZ, 1.7f * e, 0.1f, 2.0f * e, 1, 0.5f, 0.15f, chata.angulo);
        }
        r.cajaGirada(auto.x, 40, auto.z, 2.4f * e, 0.1f, 3.6f * e, 0.1f, 0.95f, 1, auto.angulo);
        float puntaX = auto.x + auto.frenteX() * 2.6f * e;
        float puntaZ = auto.z + auto.frenteZ() * 2.6f * e;
        r.cajaGirada(puntaX, 41, puntaZ, 1.3f * e, 0.1f, 1.6f * e, 1, 1, 1, auto.angulo);
    }

    private PanelHud.Datos datosHud() {
        return new PanelHud.Datos(
                auto.kmh(), Math.abs(auto.velocidad) / Auto.VELOCIDAD_MAXIMA,
                iluminacion.faros(), iluminacion.noche(), camara.modo().nombre,
                mapa.sectorEn(auto.x, auto.z).nombre(),
                entregas.completadas(), entregas.total(), entregas.nombreActivo(),
                Math.round(entregas.distancia(auto)), entregas.rumbo(auto), entregas.tiempo(),
                entregas.terminado(), entregas.mostrandoAviso(),
                minimapa.visible(), minimapa.recuadro(ancho, alto), trafico.vehiculos().size(),
                chata.enganchada(), avisoSinEspacio > 0);
    }

    // ===================== Indicador basico en el titulo de la ventana =====================

    private void actualizarTitulo(float deltaTime) {
        relojTitulo -= deltaTime;
        if (relojTitulo > 0) {
            return;
        }
        relojTitulo = 0.25f; // 4 veces por segundo: legible y actualizado
        StringBuilder t = new StringBuilder(TITULO);
        if (pausado) {
            t.append(" | PAUSA");
        }
        t.append(" | ").append(auto.kmh()).append(" km/h");
        t.append(" | ").append(iluminacion.noche() ? "Noche" : "Dia");
        t.append(" | Faros ").append(iluminacion.faros() ? "ON" : "OFF");
        t.append(" | Camara: ").append(camara.modo().nombre);
        t.append(" | Chata ").append(chata.enganchada() ? "SI" : "NO");
        if (entregas.terminado()) {
            t.append(" | GANASTE en ").append((int) entregas.tiempo()).append(" s! R: jugar otra vez");
        } else {
            t.append(" | Entregas ").append(entregas.completadas()).append("/").append(entregas.total());
            t.append(" | Destino: ").append(entregas.nombreActivo());
            t.append(" (").append(Math.round(entregas.distancia(auto))).append(" m ").append(entregas.rumbo(auto)).append(")");
            t.append(" | ").append((int) entregas.tiempo()).append(" s");
        }
        glfwSetWindowTitle(ventana, t);
    }

    /** Prueba del framebuffer size callback: cambia el tamano como si el usuario arrastrara el borde. */
    private void redimensionarDemo(int cuadro) {
        String tamano = System.getProperty("demo.redimensionar");
        if (tamano != null && cuadro == 30) {
            String[] wh = tamano.split("x");
            glfwSetWindowSize(ventana, Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
        }
    }

    // ===================== Captura PNG (solo para pruebas) =====================

    private void guardarCaptura(String ruta) {
        ByteBuffer pixeles = BufferUtils.createByteBuffer(ancho * alto * 4);
        glReadPixels(0, 0, ancho, alto, GL_RGBA, GL_UNSIGNED_BYTE, pixeles);
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int i = (x + (alto - 1 - y) * ancho) * 4; // OpenGL guarda las filas de abajo hacia arriba
                int rgb = (pixeles.get(i) & 0xff) << 16 | (pixeles.get(i + 1) & 0xff) << 8 | (pixeles.get(i + 2) & 0xff);
                imagen.setRGB(x, y, rgb);
            }
        }
        try {
            ImageIO.write(imagen, "png", new File(ruta));
        } catch (Exception e) {
            System.err.println("No se pudo guardar la captura: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new AppConduccion().run();
    }
}
