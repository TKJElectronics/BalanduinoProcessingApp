final int maxAngle = 360;
final int minAngle = 0;

void drawGraph() {
  background(255); // Set background to white
  
  for (int i = 0;i<gyro.length;i++) { // Draw graph paper
    stroke(200); // Grey
    line(337+i*10, 0, 337+i*10, height);
    line(337, i*10, 337+width, i*10);
  }
  
  stroke(0); // Black
  for (int i = 1; i <= 3; i++)
    line(337, height/4*i, width, height/4*i); // Draw lines indicating 90 deg, 180 deg, and 270 deg
  
  if (stringGyro != null)
    gyro[gyro.length-1] = map(float(trim(stringGyro)), minAngle, maxAngle, 0, height); // Convert to an float and map to the screen height, then save in buffer
  if (stringAcc != null)
    acc[acc.length-1] = map(float(trim(stringAcc)), minAngle, maxAngle, 0, height); // Convert to an float and map to the screen height, then save in buffer
  if (stringKalman != null)
    kalman[kalman.length-1] = map(float(trim(stringKalman)), minAngle, maxAngle, 0, height); // Convert to an float and map to the screen height, then save in buffer
  
  noFill();
  
  // Draw acceleromter x-axis
  stroke(255,0,0); // Red
  beginShape();
  for (int i = 0; i<acc.length;i++)
    vertex(i*6+337,height-acc[i]);
  endShape();
  
  // Draw gyro x-axis
  stroke(0,255,0); // Green
  beginShape();
  for (int i = 0; i<gyro.length;i++)
    vertex(i*6+337,height-gyro[i]);
  endShape();
  
  // Draw kalman filter x-axis
  stroke(0,0,255); // Blue
  beginShape();
  for (int i = 0; i<kalman.length;i++)
    vertex(i*6+337,height-kalman[i]);
  endShape();
  
  for (int i = 1; i<acc.length;i++) { // Put all data one array back
    acc[i-1] = acc[i];
    gyro[i-1] = gyro[i];
    kalman[i-1] = kalman[i];
  }
}
