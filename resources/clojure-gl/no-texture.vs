attribute vec4 vVertex;
uniform mat4 mvMatrix;
varying vec4 color;

void main(void) {
 gl_Position = mvMatrix * vVertex;
 color = vVertex;
}