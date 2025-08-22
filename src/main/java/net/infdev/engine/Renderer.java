// package net.infdev.engine;

// import static org.lwjgl.glfw.GLFW.*;
// import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL15.*;
// import static org.lwjgl.opengl.GL20.*;
// import static org.lwjgl.opengl.GL30.*;
// import static org.lwjgl.system.MemoryUtil.*;


// import java.io.IOException;
// import java.io.InputStream;
// import java.nio.ByteBuffer;
// import java.nio.FloatBuffer;
// import java.nio.channels.Channels;
// import java.nio.channels.ReadableByteChannel;

// import org.joml.Matrix4f;
// import org.joml.Vector3f;
// import org.lwjgl.BufferUtils;
// import org.lwjgl.opengl.GL;
// import org.lwjgl.stb.STBTTAlignedQuad;
// import org.lwjgl.stb.STBTTBakedChar;
// import static org.lwjgl.stb.STBTruetype.*;

// import net.infdev.Main;
// import net.infdev.block.Block;
// import net.infdev.block.Blocks;
// import net.infdev.util.WorldGen;
// import net.infdev.util.TextRenderer;


// public class Renderer {
//     public long window;
//     public int shaderProgram;
//     public int vao;
//     int mvpLoc;
//     float angle = 0.0f;
//     int indicesCount;
// 	// Camera state
// 	public Vector3f cameraPos = new Vector3f(0.0f, 0.0f, 3.0f);
// 	Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
// 	Vector3f cameraUp    = new Vector3f(0.0f, 1.0f, 0.0f);
// 	float cameraSpeed = 0.1f; // tweak for faster/slower movement
// 	float yaw   = -90.0f; // start facing -Z
// 	float pitch = 0.0f;

// 	float lastX = 400, lastY = 300; // window center
// 	boolean firstMouse = true;

// 	float sensitivity = 0.1f; // mouse sensitivity

//     private TextRenderer textRenderer;

//     public void init() {
//         if (!glfwInit()) {
//             throw new IllegalStateException("Unable to init GLFW");
//         }

//         glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
//         glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
//         glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

//         window = glfwCreateWindow(800, 600, "Infdev 0.1.0-alpha.1", NULL, NULL);
        
//         if (window == NULL) {
//             throw new RuntimeException("Failed to create window");
//         }

//         glfwMakeContextCurrent(window);
//         glfwSwapInterval(1);
//         glfwShowWindow(window);
//         glfwFocusWindow(window);
// 		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
// 		glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
// 			if (firstMouse) {
// 				lastX = (float) xpos;
// 				lastY = (float) ypos;
// 				firstMouse = false;
// 			}

// 			float xoffset = (float) xpos - lastX;
// 			float yoffset = lastY - (float) ypos; // reversed (y-coordinates go bottom->top)
// 			lastX = (float) xpos;
// 			lastY = (float) ypos;

// 			xoffset *= sensitivity;
// 			yoffset *= sensitivity;

// 			yaw   += xoffset;
// 			pitch += yoffset;

// 			// clamp pitch so camera doesn't flip
// 			if (pitch > 89.0f) {
//                 pitch = 89.0f;
//             } else if (pitch < -89.0f) {
//                 pitch = -89.0f;
//             }

// 			// recalc cameraFront
// 			Vector3f front = new Vector3f();

// 			front.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
// 			front.y = (float) Math.sin(Math.toRadians(pitch));
// 			front.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
            
// 			cameraFront.set(front.normalize());
// 		});


//         GL.createCapabilities();

//         Block vertices = new Block();
//         float[] color = {1, 0, 0};
//         vertices.setCubeColor(color);
//         vertices.registerCube(-0.5f, -0.5f, -0.5f, -0.5f + 1, -0.5f + 1, -0.5f + 1);

//         int[] indices = {
//             0, 2, 1,   2, 0, 3,
//             4, 5, 6,   6, 7, 4,
//             0, 1, 5,   5, 4, 0,
//             3, 7, 6,   6, 2, 3,
//             0, 4, 7,   7, 3, 0,
//             1, 2, 6,   6, 5, 1
//         };


//         indicesCount = indices.length;

//         vao = glGenVertexArrays();   
//         glBindVertexArray(vao);      

//         int vbo = glGenBuffers();                         
//         glBindBuffer(GL_ARRAY_BUFFER, vbo);               
//         glBufferData(GL_ARRAY_BUFFER, vertices.cube, GL_STATIC_DRAW); 

//         int ebo = glGenBuffers();                              
//         glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);            
//         glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW); 

//         glVertexAttribPointer(
//             0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0
//         );
//         glEnableVertexAttribArray(0);

//         glVertexAttribPointer(
//             1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES
//         );
//         glEnableVertexAttribArray(1);

//         String vertexShaderSource =
//             "#version 330 core\n" +
//             "layout (location = 0) in vec3 aPos;\n" +
//             "layout (location = 1) in vec3 aColor;\n" +
//             "out vec3 ourColor;\n" +
//             "uniform mat4 mvp;\n" +
//             "void main() {\n" +
//             "    gl_Position = mvp * vec4(aPos, 1.0);\n" +
//             "    ourColor = aColor;\n" +
//             "}\n";

//         int vertexShader = glCreateShader(GL_VERTEX_SHADER);
//         glShaderSource(vertexShader, vertexShaderSource);
//         glCompileShader(vertexShader);

//         if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
//             throw new RuntimeException("Vertex Shader compilation failed:\n" + glGetShaderInfoLog(vertexShader));
//         }

//         String fragmentShaderSource =
//             "#version 330 core\n" +
//             "uniform vec4 cubeColor;\n" +
//             "out vec4 FragColor;\n" +
//             "void main() {\n" +
//             "    FragColor = cubeColor;\n" +
//             "}\n";

//         int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
//         glShaderSource(fragmentShader, fragmentShaderSource);
//         glCompileShader(fragmentShader);

//         if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
//             throw new RuntimeException("Fragment Shader compilation failed:\n" + glGetShaderInfoLog(fragmentShader));
//         }

//         shaderProgram = glCreateProgram();
//         glAttachShader(shaderProgram, vertexShader);
//         glAttachShader(shaderProgram, fragmentShader);
//         glLinkProgram(shaderProgram);

//         if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
//             throw new RuntimeException("Program linking failed:\n" + glGetProgramInfoLog(shaderProgram));
//         }

//         glUseProgram(shaderProgram);

//         mvpLoc = glGetUniformLocation(shaderProgram, "mvp");

//         Matrix4f projection = new Matrix4f()
//             .perspective((float) Math.toRadians(45.0f), 800f / 600f, 0.1f, 100.0f);
//         Matrix4f view = new Matrix4f()
//             .lookAt(new Vector3f(0.0f, 0.0f, 3.0f),
//                     new Vector3f(0.0f, 0.0f, 0.0f),
//                     new Vector3f(0.0f, 1.0f, 0.0f));
//         Matrix4f model = new Matrix4f().identity();

//         Matrix4f mvp = new Matrix4f();
//         projection.mul(view, mvp);
//         mvp.mul(model);

//         FloatBuffer fb = BufferUtils.createFloatBuffer(16);
//         mvp.get(fb);
//         glUniformMatrix4fv(mvpLoc, false, fb);

//         glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        
// 		WorldGen.init();

//         textRenderer = new TextRenderer();
//         try {
//             textRenderer.init("/fonts/Roboto.ttf", 24);
//         } catch (IOException e) {
//             e.printStackTrace();
//         }

//         glEnable(GL_TEXTURE_2D); 
//         glEnable(GL_CULL_FACE);
//         glCullFace(GL_BACK);
//         glFrontFace(GL_CCW);
//         glEnable(GL_BLEND);
//         glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//         glDeleteShader(vertexShader);
//         glDeleteShader(fragmentShader);

//         glUseProgram(shaderProgram);
//     }

    
//     public int targetFps;
//     public int targetUps;
//     public float deltaUpdate;

//     public void loop() {
//         glEnable(GL_DEPTH_TEST);
//             long initialTime = System.currentTimeMillis();
//             float timeU = 1000.0f / Main.gameEng.targetUps;
//             float timeR = Main.gameEng.targetFps > 0 ? 1000.0f / Main.gameEng.targetFps : 0;
//             deltaUpdate = 0;
//             float deltaFps = 0;

//             long updateTime = initialTime;

//         while (!glfwWindowShouldClose(window)) {
//             glfwPollEvents(); 
//             CheckKeyPress.checkKeyPress(window, cameraPos, cameraFront, cameraUp, cameraSpeed);

//             glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

//             Matrix4f projection = new Matrix4f()
//                 .perspective((float) Math.toRadians(90.0f), 800f / 600f, 0.1f, 100.0f);
            
//             Matrix4f view = new Matrix4f()
// 								.lookAt(
// 									cameraPos,                                  // dynamic camera position
// 									new Vector3f(cameraPos).add(cameraFront),   // look towards where the camera faces
// 									cameraUp                                    // up vector
// 								);

// 			WorldGen.loop(projection,view,mvpLoc,indicesCount,shaderProgram,vao);

//             glUseProgram(shaderProgram);
//             glBindVertexArray(vao);


//             long now = System.currentTimeMillis();
//             deltaUpdate += (now - initialTime) / timeU;
//             deltaFps += (now - initialTime) / timeR;

//             // Main.gameEng.physics.applyPhysics(deltaUpdate, cameraPos, cameraUp, cameraSpeed);


//             if (targetFps <= 0 || deltaFps >= 1) {
//                 //appLogic.input(window, scene, now - initialTime);
//             }

//             if (deltaUpdate >= 1) {
//                 long diffTimeMillis = now - updateTime;
//                 //appLogic.update(window, scene, diffTimeMillis);
//                 updateTime = now;
//                 deltaUpdate--;
//             }

//             if (targetFps <= 0 || deltaFps >= 1) {
//                 deltaFps--;
//             }
//             initialTime = now;
            
//             //Collision.checkCollision();

//             glfwSwapBuffers(window);

//             glDisable(GL_DEPTH_TEST);
//             float[] ortho = new Matrix4f().ortho2D(0, 800, 600, 0).get(new float[16]);
//             textRenderer.renderText("Hello World", 25, 50, 1.0f, 1f, 1f, 1f, ortho);
//             glEnable(GL_DEPTH_TEST);

//             // glfwSwapBuffers(window);
//         }
//     }


// }
