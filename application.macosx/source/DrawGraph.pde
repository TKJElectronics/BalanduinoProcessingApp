//convert all axis
final int minAngle = 0;
final int maxAngle = 360;

void drawGraph() {
  background(255); // white  
  for (int i = 0;i<gyro.length;i++) {
    stroke(200); // gray
    line(337+i*10, 0, 337+i*10, height);
    line(337, i*10, 337+width, i*10);
  }
  
  stroke(0); // black
  for (int i = 1; i <= 3; i++)
    line(337, height/4*i, width, height/4*i); // Draw line, indicating 90 deg, 180 deg, and 270 deg
  convert();
  drawAxis();
}

void convert() {
  /* convert the gyro x-axis */
  if (stringGyro != null) {
    // trim off any whitespace:
    stringGyro = trim(stringGyro);
    // convert to an float and map to the screen height, then save in buffer:    
    gyro[gyro.length-1] = map(float(stringGyro), minAngle, maxAngle, 0, height);
  }  
  /* convert the accelerometer y-axis */
  if (stringAcc != null) {
    // trim off any whitespace:
    stringAcc = trim(stringAcc);
    // convert to an float and map to the screen height, then save in buffer:        
    acc[acc.length-1] = map(float(stringAcc), minAngle, maxAngle, 0, height);
  }
  /* convert the kalman filter y-axis */
  if (stringKalman != null) {
    // trim off any whitespace:
    stringKalman = trim(stringKalman);
    // convert to an float and map to the screen height, then save in buffer:    
    kalman[kalman.length-1] = map(float(stringKalman), minAngle, maxAngle, 0, height);
  }
}

void drawAxis() { 
  /* draw acceleromter x-axis */
  noFill();
  stroke(255,0,0); //red
  // redraw everything
  beginShape();
  for(int i = 0; i<acc.length;i++)
    vertex(i*6+337,height-acc[i]);
  endShape();
  // put all data one array back
  for(int i = 1; i<acc.length;i++)
    acc[i-1] = acc[i];  
   
  /* draw gyro x-axis */
  noFill();
  stroke(0,255,0); // green
  // redraw everything
  beginShape();
  for(int i = 0; i<gyro.length;i++)
    vertex(i*6+337,height-gyro[i]);
  endShape();
  // put all data one array back
  for(int i = 1; i<gyro.length;i++)
    gyro[i-1] = gyro[i];
    
  /* draw kalman filter x-axis */
  noFill();
  stroke(0,0,255); // blue  
  // redraw everything
  beginShape();
  for(int i = 0; i<kalman.length;i++)
    vertex(i*6+337,height-kalman[i]);
  endShape();
  // put all data one array back
  for(int i = 1; i<kalman.length;i++)
    kalman[i-1] = kalman[i];
}
