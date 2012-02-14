attribute vec4 vVertex;
attribute vec4 normal;

uniform mat4 mvMatrix;
varying vec4 color;
varying vec4 vertNormal;

void main(void) {
 gl_Position = mvMatrix * vVertex;
 color = vVertex;
 vertNormal = normal;
}