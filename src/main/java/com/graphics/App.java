package com.graphics;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Este programa hace 3 cosas:
 * 1. Abrir una ventana usando la biblioteca de GLFW
 * 2. Definir un triangulo y usos shaders
 * 3. En un bucle, dibujar el triangulo en la ventana una y otra vez
 * 
 * CONCEPTOS CLAVES:
 * - OpenGL: conjunto de funciones para dibujar en pantalla usando al GPU
 * - GLWF: bibliote que crea la venta y maneja el teclado/raton. OpenGL solo
 * dice como dibujar
 * no crea ventanas
 * - GPU: tarjeta grafica
 * - Shader: codigo que dice como transformar vertice y q color dar a cada
 * pixel. Se escribe en
 * lenguaje GLSL
 * - VAO/VBO: formas de guardar en la GPU los datos de los vertices
 */
public class App {

    // referencia a nuestra ventana
    private long window;

    // Nuestro programa de shader: es el vertex shader + fragment shader unidos.
    // La GPU usa este programa para dibujar cada frame
    private int programa;

    // VAO = Vertex Array Object: es como un dosificador que queda toda la
    // configuracion
    private int vao;

    // VBO = Vertex Buffer Object: es un bloque de memora en la GPU donde guardamos
    // las posiciones de los vertices
    private int vbo;

    private static final int ANCHO = 800;
    private static final int ALTO = 600;

    /**
     * Punto de entrada: inicializamos todo, ejecutamos el bucle del dibujo y al
     * salir limpiamos
     */
    public void run() {
        init();
        loop();
        cleanup();
    }

    // PASO 1: Incializar GLFW y crear la ventana
    // GLFW (Graphics Library Framework) es una biblioteca que:
    // - Crea y gestiona la ventana del sistema
    // - Recibe igual eventos, teclado, raton, ventana
    public void init() {
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
        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "OpenGL - Triangulo", 0, 0);

        // Decir que todas las ordenes de OPENGL que ejecutamos a partir de ahora
        // se aplique a esta venta
        GLFW.glfwMakeContextCurrent(window);

        // Sincronizar el dibujo con el monitor (1 = esperar al siguiente refresco)
        // Evitamos el "tearing" (imagen partida)
        GLFW.glfwSwapInterval(1);
        // hacer visible la ventana
        GLFW.glfwShowWindow(window);

        // Carga en java las funciones de OpenGl
        // LWJGL necesita este paso para enlazar las llamadas de Java con la GPU
        GL.createCapabilities();

        crearShaders();
        crearTriangulo();
    }

    // =========================================================
    // Paso 2: CREAR LOS SHADERS (progrmas que corren en la GPU)
    // =========================================================
    // ¿Porqué existen dos shaders?
    // En OpenGL la GPU dibuja en dos etapas:
    // 1. VERTEX SHADER: se ejecuta por cada VERTICE (en nuestro triangulo, 3 veces)
    // Su trabajo es decir donde va ese vertice en pantalla (posición 2D/3D)
    // 2. FRAGMENT SHADER: se ejecuta por cada PIXEL que ocupa el triangulo (miles y
    // miles)
    // Su trabajo es decir de que color se pinta ese pixel.
    // Sin el vertex shader, la GPU no sabria donde colocar los puntos. Sin el
    // fragment shader no
    // sabria que color dar a cada pixel. Por eso se requiere los dos.
    //
    // GLSL (OpenGL Shading Language). Es un lenguaje parecido a C, porque se usa
    // para
    // programar la GPU
    //
    private void crearShaders() {
        // vertexSrc: codigo GLSL del vertex shader
        // Recibe la posicion de cada vertice (aPos) y la asigna a gl_Position (salida
        // estandar)
        // "layout (location = 0) in vec3 aPos" = entrada en el canal 0, 3 floats (x, y,
        // z)
        String vertexSrc = "#version 330 core\n"
                + "layout (location = 0) in vec3 aPos;\n"
                + "void main() { gl_Position = vec4(aPos, 1.0); }\n";

        // fragmentSrc: codigo GLSL del fragment shader
        // fragColor = color de salida del pixel. vec4(R, G, B, A) = rojo, verde, azul,
        // alpha
        // Se ejecuta por cada pixel del triangulo. la salida es el color del pixel
        // vec4(0.2, 0.8, 0.4, 1.0) = verde
        String fragmentSrc = "#version 330 core\n"
                + "out vec4 fragColor;\n"
                + "void main() { fragColor = vec4(0.2, 0.8, 0.4, 1.0); }\n";

        // Crear el objeto en la GPU y compilar el codigo
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

        // Crear el objeto en la GPU y compilar el codigo
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment");

        // Crear el progrma (vertex + fragment unidos)
        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        // Comprobar que el enlace fue correcto
        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar programa: " + GL20.glGetProgramInfoLog(programa));
        }

        // Los shaders ya estan copiados dentro del programa, podemos borrarlos para
        // liberar
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void comprobarShader(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    // =========================================================
    // PASO 3: Crear el triangulo
    // =========================================================
    // Un triangulo son 3 puntos (vertices). Cada vertice tiene una posición (x, y,
    // z).
    // En OpenGL las coordenadas suelen ir de -1 a 1:(-1,-1) = esquna inferior
    // izquierda,
    // (1,1) = esquina superior derecha. El centro es (0,0)
    private void crearTriangulo() {
        float[] vertices = {
                0.0f, 0.5f, 0.0f, // vertice 1: arriba al centro
                -0.5f, -0.5f, 0.0f, // vertice 2: abajo a la izquierda
                0.5f, -0.5f, 0.0f, // vertice 2: abajo a la derecha
        };

        // VAO: objeto que guarda la configuración de los buffers y atributos
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // VBO: buffer en la GPU donde guardamos las coordenadas
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // OpenGL espera los datos en FloatBuffer, no en un float[] de java
        // copiamos el array al buffer y flip() dejamos listos para la lectura.
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        // Enviar los datos al VBO. GL_STATIC_DRAW = "no vamos a cambiar estos datos"
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // Decir a OpenFL como voy a leer este buffer
        // atributo location 0 (como layout (location = 0) en el vertex shader)
        // 3 floats por vertice (x, y, z)
        // tipo GL_FLOAT, sin normalizar
        // stride = 12 bytes (3 floats x 4 byte), offset = 0
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        // Activar el atributo 0 (en el shader es in vec3 aPos en location 0)
        GL20.glEnableVertexAttribArray(0);

        // Desenlazar el VBO y VAO. Porque ya no hace falta dejarlos activos
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    // BUCLE PRINCIPAL: en cada iteracion dibujamos un frame
    // Mientras la ventana no se cierre:
    // 1. Limpiamos la ventana
    // 2. Decimos usar este programa de shaders y este VAO para dibujar
    // 3. Ordenamos dibujar los 3 vertics
    // 4. Intercambiamos los buffers
    // 5. Procesamos los eventos
    private void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            // Color con el que vamos al limpiar la pantalla (gris oscuro: RGBA)
            GL11.glClearColor(0.08f, 0.08f, 0.12f, 1.0f);
            // Limpiar el buffer de color (rellenar con el color anterior)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            // Activar nuestro programa de shaders
            GL20.glUseProgram(programa);
            // Activar el VAO que contiene el triangulo
            GL30.glBindVertexArray(vao);
            // Orden del dibujo: modo, empezar con el vertice 0 y dibujar 3 vertices
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

            // Doble buffer: hemos dibujado en un buffer oculto y ahora lo mostramos en
            // pantalla e intercambiamos
            GLFW.glfwSwapBuffers(window);
            // Procesamos eventos del sistema (si el usuario cerro ventana, si hizo click,
            // etc)
            GLFW.glfwPollEvents();
        }
    }

    private void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new App().run();
    }
}
