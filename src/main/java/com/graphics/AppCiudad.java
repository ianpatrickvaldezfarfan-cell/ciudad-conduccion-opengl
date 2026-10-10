package com.graphics;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class AppCiudad {
    private long ventana;
    private int programa;
    private int vao;
    private int vbo;
    private int ancho = 1100;
    private int alto = 760;

    // Angulo inicial de la camara de la ciudad
    private float orbita = 0.6f;

    // Cambiar el angulo o la vista del mapa
    private boolean vistaMapa = false;

    // Distancia del origen a cada borde
    private static final float LIMITE = 35;

    // Ancho y profundidad de cada celda del mapa
    private static final float CELDA = 10;

    private final Map<String, Integer> uniforms = new HashMap<>();

    private static final int[][] MAPA = {
            { 0, 0, 0, 0, 0, 0, 0 },
            { 0, 1, 0, 1, 0, 1, 0 },
            { 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 0, 1, 0, 1, 0 },
            { 0, 0, 0, 0, 0, 0, 0 },
            { 0, 1, 0, 2, 0, 1, 0 },
            { 0, 0, 0, 0, 0, 0, 0 }
    };

    // Organizar las tres fases de la aplicación y limpiar en caso ocurra un error
    public void run() {
        // Preparamos mensajes de error en la consola
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err);
        // Instamos el manejador de errores en GLFW
        errores.set();
        try {
            iniciar();
            loop();
        } finally {
            limpiar();
            glfwSetErrorCallback(null);
            errores.free();
        }
    }

    // Configurar GLFW y OpenGL igual que las clases de App
    private void iniciar() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        // Reestablece todas las opciones por defecto
        GLFW.glfwDefaultWindowHints();

        // Ocultar la ventana al principio; la mostrar recien cuando OpenGL este listo
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        // Permitir que el usuario pueda redimensionar la pantalla
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        // Queremos que use la version de OpenGL 3.3
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        // Solo funciones modernas de OpenGL (evitar compatibilidad con codigo muy
        // antiguo)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        // MacOS hay que reactivar para usar el core profile
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        // Creamos la ventana: ancho, alto, tiitulo, monitor(0 = ventana normal),
        // ventana para compartir
        ventana = GLFW.glfwCreateWindow(ancho, alto, "Ciudad | Flechas: orbitar | Esc: Salir", 0, 0);

        // Decir que todas las ordenes de OPENGL que ejecutamos a partir de ahora
        // se aplique a esta venta
        GLFW.glfwMakeContextCurrent(ventana);

        // Sincronizar el dibujo con el monitor (1 = esperar al siguiente refresco)
        // Evitamos el "tearing" (imagen partida)
        GLFW.glfwSwapInterval(1);
        // hacer visible la ventana
        GLFW.glfwShowWindow(ventana);

        // Carga en java las funciones de OpenGl
        // LWJGL necesita este paso para enlazar las llamadas de Java con la GPU
        GL.createCapabilities();

        // Hace que las superficie cercana oculten a las lejanas
        glEnable(GL_DEPTH_TEST);

        // Registramos la funcion del evento del teclado para cerrar la ventana
        glfwSetKeyCallback(ventana, (ventanaEvento, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                tecla(key);
            }
        });

        crearPrograma();
        crearCubo();
    }

    private void loop() {
        // Guardamos el instante inicial para calcular el tiempo entre cuadros
        double tiempoAnterior = glfwGetTime();
        // Cuenta los cuadros dibujados para la comprobación
        int cuadros = 0;
        // Cero permite jugar; otro valor limita el arranque de prueba
        int maxCuadros = Integer.getInteger("demo.frames", 0);
        // Reserva espacio para que GLFW escriba el ancho en pixeles
        int[] anchoReal = new int[1];
        // Reserva espacio para qie GLFW escriba el alto en pixeles
        int[] altoReal = new int[1];

        while (!glfwWindowShouldClose(ventana)) {
            // Procesa el teclado, redimensionamiento y boton de cierre
            glfwPollEvents();
            // Consulta el tiempo actual en segundos
            double tiempoActual = glfwGetTime();
            float deltaTime = (float) Math.min(tiempoActual - tiempoAnterior, 0.05);
            tiempoAnterior = tiempoActual;
            actualizar(deltaTime);
            // Pedimos a GLFW el tamaño real en pixeles (cambia al redimensionar)
            glfwGetFramebufferSize(ventana, anchoReal, altoReal);
            ancho = anchoReal[0];
            alto = altoReal[0];
            if (ancho > 0 && alto > 0) {
                dibujarFrame();
            }
            glfwSwapBuffers(ventana);
            cuadros++;
            if (maxCuadros > 0 && cuadros >= maxCuadros) {
                glfwSetWindowShouldClose(ventana, true);
            }
        }
    }

    private void limpiar() {
        if (programa != 0) {
            glDeleteProgram(programa);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (ventana != 0) {
            Callbacks.glfwFreeCallbacks(ventana);
            glfwDestroyWindow(ventana);
        }
        glfwTerminate();
    }

    // ===================== Teclas y camaras ===================

    /** Atiende las acciones de una sola pulsación */
    private void tecla(int key) {
        if (key == GLFW_KEY_ESCAPE) {
            glfwSetWindowShouldClose(ventana, true);
        }
    }

    private boolean pulsada(int key) {
        return glfwGetKey(ventana, key) == GLFW_PRESS;
    }

    /** Mueve la camara alrededor de la ciudad con las flechas */
    private void actualizar(float deltaTime) {
        if (pulsada(GLFW_KEY_LEFT)) {
            orbita -= deltaTime; // Reducimos el angulo
        }

        if (pulsada(GLFW_KEY_RIGHT)) {
            orbita += deltaTime; // Aumentamos el angulo
        }
    }

    private void configurarCamara() {
        float camaraX = (float) Math.sin(orbita) * 65;
        float camaraZ = (float) Math.cos(orbita) * 65;

        vector("uOjo", camaraX, 55, camaraZ);
        vector("uObjetivo", 0, 0, 0);
        decimal("uAspecto", (float) ancho / alto);
    }

    private void prepararLuces() {
        // Enviar las luces al GPU
    }

    // ===================== Ciudad a partir de la matriz ===================
    private float centro(int indice) {
        return -LIMITE + CELDA * (indice + 0.5f);
    }

    private void dibujarFrame() {
        glViewport(0, 0, ancho, alto);
        glClearColor(0.12f, 0.20f, 0.30f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(programa);
        glBindVertexArray(vao);
        entero("uMapa", 0);
        configurarCamara();
        prepararLuces();
        escena();
        if (glGetError() != GL_NO_ERROR) {
            throw new IllegalStateException("Error OpenGL al dibujar");
        }
    }

    private void escena() {
        caja(0, -0.25f, 0, 70, 0.5f, 70, 0.16f, 0.19f, 0.23f);
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                float x = centro(columna);
                float z = centro(fila);
                int tipo = MAPA[fila][columna];
                if (tipo == 0) {
                    dibujarMarcasCalle(fila, columna, x, z);
                } else {
                    caja(x, 0.15f, z, 10, 0.3f, 10, 0.60f, 0.64f, 0.66f);
                    if (tipo == 1) {
                        float altura = 5 + (fila * 3 + columna * 7) % 9;
                        float rojo = 0.28f + columna * 0.045f;
                        float azul = 0.48f + fila * 0.025f;
                        caja(x, altura / 2 + 0.3f, z, 7, altura, 7, rojo, 0.40f, azul);
                        caja(x, altura + 0.45f, z, 7.3f, 0.3f, 7.3f, 0.20f, 0.26f, 0.32f);
                    } else {
                        caja(x, 0.32f, z, 9, 0.1f, 9, 0.20f, 0.45f, 0.28f);
                    }
                }
            }
        }
    }

    private void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        if (fila % 2 == 0 && columna % 2 == 1) {
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) {
                caja(x + desplazamiento, 0.025f, z, 1.6f, 0.03f, 0.13f, 1, 0.84f, 0.35f);
            }
        }

        if (columna % 2 == 0 && fila % 2 == 1) {
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) {
                caja(x, 0.025f, z + desplazamiento, 0.13f, 0.03f, 0.13f, 1, 0.84f, 0.35f);
            }
        }
    }

    // ===================== Dibujar cajas y enviar uniform ===================
    private void caja(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0);
    }

    private void cajaGirada(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b,
            float angulo) {
        vector("uPos", x, y, z);
        vector("uEscala", sx, sy, sz);
        vector("uColor", r, g, b);
        decimal("uGiro", angulo);
        glDrawArrays(GL_TRIANGLES, 0, 36);
    }

    private int uniform(String nombre) {
        if (!uniforms.containsKey(nombre)) {
            int ubicacion = glGetUniformLocation(programa, nombre);
            uniforms.put(nombre, ubicacion);
        }
        return uniforms.get(nombre);
    }

    private void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z);
    }

    private void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor);
    }

    private void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor);
    }

    // ===================== Programa de shaders y cubo ===================

    /** Compila los dos shaders y los une en un programa */
    private void crearPrograma() {
        int vertex = compilarShader(GL_VERTEX_SHADER, vertexShader(), "Vertex");
        int fragment = compilarShader(GL_FRAGMENT_SHADER, fragmentShader(), "Fragment");

        programa = glCreateProgram();
        glAttachShader(programa, vertex);
        glAttachShader(programa, fragment);
        glLinkProgram(programa);
        if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error al enlazar programa: " + glGetProgramInfoLog(programa));
        }

        // Ya estan copiados dentro del programa
        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    private int compilarShader(int tipo, String codigo, String nombre) {
        int shader = glCreateShader(tipo);
        glShaderSource(shader, codigo);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException(nombre + " shader: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    /**
     * Cubo de lado 1 centrado en el origen: 6 caras x 2 triangulos x 3 vertices = 36.
     * Cada vertice tiene posicion (x, y, z) y normal (nx, ny, nz).
     * Con uEscala y uPos el shader lo convierte en cualquier caja de la ciudad.
     */
    private void crearCubo() {
        float[] vertices = {
                // Cara frontal (z = +0.5)
                -0.5f, -0.5f, 0.5f, 0, 0, 1,
                0.5f, -0.5f, 0.5f, 0, 0, 1,
                0.5f, 0.5f, 0.5f, 0, 0, 1,
                0.5f, 0.5f, 0.5f, 0, 0, 1,
                -0.5f, 0.5f, 0.5f, 0, 0, 1,
                -0.5f, -0.5f, 0.5f, 0, 0, 1,
                // Cara trasera (z = -0.5)
                0.5f, -0.5f, -0.5f, 0, 0, -1,
                -0.5f, -0.5f, -0.5f, 0, 0, -1,
                -0.5f, 0.5f, -0.5f, 0, 0, -1,
                -0.5f, 0.5f, -0.5f, 0, 0, -1,
                0.5f, 0.5f, -0.5f, 0, 0, -1,
                0.5f, -0.5f, -0.5f, 0, 0, -1,
                // Cara izquierda (x = -0.5)
                -0.5f, -0.5f, -0.5f, -1, 0, 0,
                -0.5f, -0.5f, 0.5f, -1, 0, 0,
                -0.5f, 0.5f, 0.5f, -1, 0, 0,
                -0.5f, 0.5f, 0.5f, -1, 0, 0,
                -0.5f, 0.5f, -0.5f, -1, 0, 0,
                -0.5f, -0.5f, -0.5f, -1, 0, 0,
                // Cara derecha (x = +0.5)
                0.5f, -0.5f, 0.5f, 1, 0, 0,
                0.5f, -0.5f, -0.5f, 1, 0, 0,
                0.5f, 0.5f, -0.5f, 1, 0, 0,
                0.5f, 0.5f, -0.5f, 1, 0, 0,
                0.5f, 0.5f, 0.5f, 1, 0, 0,
                0.5f, -0.5f, 0.5f, 1, 0, 0,
                // Cara superior (y = +0.5)
                -0.5f, 0.5f, 0.5f, 0, 1, 0,
                0.5f, 0.5f, 0.5f, 0, 1, 0,
                0.5f, 0.5f, -0.5f, 0, 1, 0,
                0.5f, 0.5f, -0.5f, 0, 1, 0,
                -0.5f, 0.5f, -0.5f, 0, 1, 0,
                -0.5f, 0.5f, 0.5f, 0, 1, 0,
                // Cara inferior (y = -0.5)
                -0.5f, -0.5f, -0.5f, 0, -1, 0,
                0.5f, -0.5f, -0.5f, 0, -1, 0,
                0.5f, -0.5f, 0.5f, 0, -1, 0,
                0.5f, -0.5f, 0.5f, 0, -1, 0,
                -0.5f, -0.5f, 0.5f, 0, -1, 0,
                -0.5f, -0.5f, -0.5f, 0, -1, 0,
        };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        // stride = 6 floats por vertice (posicion + normal)
        int stride = 6 * Float.BYTES;
        // location 0 = aPos: empieza en el byte 0
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        // location 1 = aNormal: empieza despues de los 3 floats de posicion
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    // ===================== Shaders: Codigo que se ejecuta en la GPU
    // ===================
    // IMPORTANTE: dentro del codigo GLSL no usar tildes ni la letra enie,
    // algunos drivers de Windows (Intel/AMD) no compilan caracteres fuera de ASCII
    private String vertexShader() {
        return """
                    #version 330 core
                    layout (location = 0) in vec3 aPos; // Leemos la posicion local del vertice desde el VBO
                    layout (location = 1)  in vec3 aNormal; // Leemos la normal de la cara desde el mismo VBO
                    uniform vec3 uPos; // Recibe el centro e la caja de la ciudad
                    uniform vec3 uEscala; // Recibe el tamanio de la caja en cada eje
                    uniform vec3 uOjo; // Recibe la posicion de la camara
                    uniform vec3 uObjetivo; // Punto que observa la camara
                    uniform float uGiro; // Recibe el giro del objeto alrededor de Y
                    uniform float uAspecto; // Recibe la relacion del ancho y alto de la imagen
                    uniform int uMapa; // Seleccionamos la perspectiva (0, 1)
                    out vec3 vMundo; // La posicion mundial al shader de fragmentos
                    out vec3 vNormal; // Enviamos la normal transformada par ala iluminacion

                    void main() {
                        float coseno = cos(uGiro); // Calculamos el coseno del giro del objeto
                        float seno = sin(uGiro);
                        mat3 giro = mat3(
                            coseno, 0.0,    -seno,
                            0.0,    1.0,    0.0,
                            seno,   0.0,    coseno
                        );
                        vMundo = giro * (aPos * uEscala) + uPos; // Escalar, firar y trasladar el vertice al mundo
                        vNormal = normalize(giro * (aNormal / uEscala)); // Corregimos la normal con la inversa transpuesta de la escala y giro

                        if (uMapa == 1) { //Esta rama se usa al dibujar el minimapa
                            float pantallaX = vMundo.x / 37.0;
                            float pantallaY = vMundo.z / 37.0;
                            float profundidad = -vMundo.y / 100.0;
                            gl_Position = vec4(pantallaX, pantallaY, profundidad, 1.0);
                        } else { // La escena principal utilizando la camara como perspectiva
                            vec3 frente = normalize(uObjetivo - uOjo); // Calcular la direccion hacia lo que mira la camara
                            vec3 derecha = normalize(cross(frente, vec3(0.0, 1.0, 0.0))); // Obtenemos el eje horizontal de la camra
                            vec3 arriba = cross(derecha, frente); // Obtenemos el eje vertical
                            vec3 diferencia = vMundo - uOjo; // Trasladamos el origen del mundo hasta la camara
                            float vistaX = dot(diferencia, derecha);
                            float vistaY = dot(diferencia, arriba);
                            float vistaZ = -dot(diferencia, frente);
                            float factor = 1.0 / tan(radians(55.0) * 0.5); // Convertimos el campo visual (55 grados) en escala de perspectiva
                            float cerca = 0.1;
                            float lejos = 250.0;
                            float clipX = vistaX * factor / uAspecto;
                            float clipY = vistaY * factor;
                            float clipZ = (lejos + cerca) / (cerca - lejos) * vistaZ;
                            clipZ += 2.0 * lejos * cerca / (cerca - lejos); // agregar la constante de profundidad
                            gl_Position = vec4(clipX, clipY, clipZ, -vistaZ);
                        }
                    }
                """;
    }

    private String fragmentShader() {
        return """
                    #version 330 core
                    in vec3 vNormal; // Normal que llega del vertex shader
                    uniform vec3 uColor;
                    out vec4 color;
                    void main() {
                        // Luz fija muy simple para distinguir las caras de las cajas
                        vec3 luz = normalize(vec3(0.4, 1.0, 0.3));
                        float difusa = max(dot(normalize(vNormal), luz), 0.0);
                        color = vec4(uColor * (0.35 + 0.65 * difusa), 1.0);
                    }
                """;
    }

    public static void main(String[] args) {
        new AppCiudad().run();
    }
}
