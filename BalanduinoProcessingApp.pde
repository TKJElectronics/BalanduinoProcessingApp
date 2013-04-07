import processing.serial.*;  
import controlP5.*;
ControlP5 controlP5;

Textfield P;
Textfield I;
Textfield D;
Textfield targetAngle;

String stringP = "";
String stringI = "";
String stringD = "";
String stringTargetAngle = "";

PFont f;

final boolean useDropDownLists = true; // Set if you want to use the dropdownlist or not
byte defaultComPort = 0;

// Dropdown list
DropdownList COMports; // Define the variable ports as a Dropdownlist.
Serial serial; // Define the variable port as a Serial object.
int portNumber = -1; // The dropdown list will return a float value, which we will connvert into an int. We will use this int for that.

boolean connectedSerial;
boolean aborted;

boolean upPressed;
boolean downPressed;
boolean leftPressed;
boolean rightPressed;
boolean sendData;

String stringGyro;
String stringAcc;
String stringKalman;

/* We will store 101 readings */
float[] acc = new float[101];
float[] gyro = new float[101];
float[] kalman = new float[101];

boolean drawValues;

void setup() {
  controlP5 = new ControlP5(this);
  size(937, 330);

  f = loadFont("EuphemiaUCAS-Bold-30.vlw");
  textFont(f, 30);

  /* For remote control */
  controlP5.addButton("Up", 0, 337/2-20, 70, 40, 20);
  controlP5.addButton("Down", 0, 337/2-20, 92, 40, 20);
  controlP5.addButton("Left", 0, 337/2-62, 92, 40, 20);
  controlP5.addButton("Right", 0, 337/2+22, 92, 40, 20);  

  /* For setting the PID values etc. */
  P = controlP5.addTextfield("P", 10, 165, 35, 20);
  P.setFocus(true);
  I = controlP5.addTextfield("I", 50, 165, 35, 20);
  D = controlP5.addTextfield("D", 90, 165, 35, 20);
  targetAngle = controlP5.addTextfield("TargetAngle", 130, 165, 35, 20);
  
  P.setInputFilter(ControlP5.FLOAT);
  I.setInputFilter(ControlP5.FLOAT);
  D.setInputFilter(ControlP5.FLOAT);
  targetAngle.setInputFilter(ControlP5.FLOAT);

  P.setAutoClear(false);
  I.setAutoClear(false);
  D.setAutoClear(false);
  targetAngle.setAutoClear(false);

  P.clear();
  I.clear();
  D.clear();
  targetAngle.clear();

  controlP5.addButton("Submit", 0, 202, 165, 60, 20);
  controlP5.addButton("Clear", 0, 267, 165, 60, 20);

  controlP5.addButton("Abort", 0, 10, 300, 40, 20);
  controlP5.addButton("Continue", 0, 55, 300, 50, 20);
  Button b = controlP5.addButton("StoreValues", 0, 175, 300, 65, 20);
  b.captionLabel().set("Store values");
  b = controlP5.addButton("PairWithWiimote", 0, 245, 275, 82, 20);
  b.captionLabel().set("Pair with Wiimote");
  b = controlP5.addButton("RestoreDefaults", 0, 245, 300, 82, 20);
  b.captionLabel().set("Restore defaults");
  
  for (int i=0;i<gyro.length;i++) { // center all variables    
    gyro[i] = height/2;
    acc[i] = height/2; 
    kalman[i] = height/2;
  }

  //println(Serial.list()); // Used for debugging
  if(useDropDownLists)
    initDropdownlist();
  else { // if useDropDownLists is false, it will connect automatically at startup
    try {
      serial = new Serial(this, Serial.list()[defaultComPort], 115200);
    } catch (Exception e) {
      //e.printStackTrace();
      println("Couldn't open serial port");
    }
    if(serial != null) {
      serial.bufferUntil('\n');
      connectedSerial = true;
      delay(100);
      serial.write("GP;"); // Get PID values
      delay(10);
      serial.write("IB;"); // Get IMU Data
    }
  }
  drawGraph(); // Draw graph at startup
}

void draw() {
  /* Draw Graph */
  if(connectedSerial && drawValues) {
    drawValues = false;
    drawGraph();
  }

  /* Remote contol */
  fill(0);
  stroke(0);
  rect(0, 0, 337, height);
  fill(0, 102, 153);
  textSize(25); 
  textAlign(CENTER); 
  text("Press buttons to steer", 337/2, 55);

  /* Set PID value etc. */
  fill(0, 102, 153);
  textSize(30); 
  textAlign(CENTER); 
  text("Set PID Values:", 337/2, 155);
  text("Current PID Values:", 337/2, 250);

  fill(255, 255, 255);
  textSize(10);  
  textAlign(LEFT);
  text("P: " + stringP + " I: " + stringI +  " D: " + stringD + " TargetAngle: " + stringTargetAngle, 10, height-50);

  if(sendData) { // Data is send as x,y-coordinates
    if(upPressed) {
      if(leftPressed)
        serial.write("CJ,-0.7,0.7;"); // Forward left
      else if(rightPressed)
        serial.write("CJ,0.7,0.7;"); // Forward right
      else
        serial.write("CJ,0,0.7;"); // Forward
    }
    else if(downPressed) {
      if(leftPressed)
        serial.write("CJ,-0.7,-0.7;"); // Backward left
      else if(rightPressed)
        serial.write("CJ,0.7,-0.7;"); // Backward right
      else
        serial.write("CJ,0,-0.7;"); // Backward
    } 
    else if(leftPressed)
      serial.write("CJ,-0.7,0;"); // Left
    else if(rightPressed)
      serial.write("CJ,0.7,0;"); // Right
    else {
      serial.write("CS;");
      println("Stop");
    }
    sendData = false;
  }
}
void Abort(int theValue) {
  if(connectedSerial) {
    serial.write("A;");
    println("Abort");
    aborted = true;
  } else
    println("Establish a serial connection first!");
}
void Continue(int theValue) {
  if(connectedSerial) {
    serial.write("C");
    println("Continue");
    aborted = false;
  } else
    println("Establish a serial connection first!");
}
void Submit(int theValue) {
  if(connectedSerial) {    
    println("PID values: " + P.getText() + " " + I.getText() + " " + D.getText() +  " TargetAnlge: " + targetAngle.getText());
    
    if(!P.getText().equals(stringP)) {
      println("Send P value");
      serial.write("SP," + P.getText() + ';');
      delay(10);
    }
    if(!I.getText().equals(stringI)) {
      println("Send I value");
      serial.write("SI," + I.getText() + ';');
      delay(10);
    }
    if(!D.getText().equals(stringD)) {
      println("Send D value");
      String output = "SD," + D.getText() + ';';
      serial.write(output);
      delay(10);
    }
    if(!targetAngle.getText().equals(stringTargetAngle)) {
      println("Send target angle");
      serial.write("ST," + targetAngle.getText() + ';');
      delay(10);
    }  
    serial.write("GP;"); // Get PID values
    delay(10);
  } else
    println("Establish a serial connection first!");
}
void Clear(int theValue) {
  P.clear();
  I.clear();
  D.clear();
  targetAngle.clear();
}
void RestoreDefaults(int theValue) {
  if(connectedSerial) {
    serial.write("CR;"); // Restore values
    println("RestoreDefaults");
    delay(10);
  } else
    println("Establish a serial connection first!");
}
void PairWithWiimote(int theValue) {
  if(connectedSerial) {
    serial.write("CW;"); // Pair with Wiimote
    println("Pair with Wiimote");
    delay(10);
  } else
    println("Establish a serial connection first!");
}
void StoreValues(int theValue) {
  //Don't set the text if the string is empty or it will crash
  if(stringP != null)
    P.setText(stringP);
  if(stringI != null)
    I.setText(stringI);
  if(stringD != null)
    D.setText(stringD);
  if(stringTargetAngle != null)
    targetAngle.setText(stringTargetAngle);
}
void serialEvent(Serial serial) {
  String[] input = trim(split(serial.readString(), ','));
  //for (int i = 0; i<input.length;i++) // Uncomment for debugging
    //println("Number: " + i + " Input: " + input[i]);

  if(input[0].equals("P")) {    
    if(input[1].length() > 5)
      stringP = input[1].substring(0, 5);
    else
      stringP = input[1];
      
    if(input[2].length() > 5)
      stringI = input[2].substring(0, 5);
    else
      stringI = input[2];
      
    if(input[3].length() > 5)
      stringD = input[3].substring(0, 5);
    else
      stringD = input[3];
      
    if(input[4].length() > 6)
      stringTargetAngle = input[4].substring(0, 6);
    else
      stringTargetAngle = input[4];
  }
  if(input[0].equals("V")) {
    stringAcc = input[1];
    stringGyro = input[2];
    stringKalman = input[3];
    
    /*print(stringAcc);
    print(stringGyro);
    print(stringKalman);*/
  }
  serial.clear();  // Empty the buffer
  drawValues = true; // Draw the graph
}
void keyPressed() {
  if(key == 's' || key == 'S')
    StoreValues(0);
  if(key == TAB) { //'\t'
    if(P.isFocus()) {
      P.setFocus(false);
      I.setFocus(true);
    } else if(I.isFocus()) {
      I.setFocus(false);
      D.setFocus(true);
    } else if(D.isFocus()) {
      D.setFocus(false);
      targetAngle.setFocus(true);
    } else if(targetAngle.isFocus()) {
      targetAngle.setFocus(false);
      P.setFocus(true);
    } else
      P.setFocus(true);
  }
  else if(key == ENTER) // '\n'
    Submit(0);
  else if(key == ESC) {
    if(aborted)
      Continue(0);
    else
      Abort(0);      
    key = 0; // Disable Processing from quiting when pressing ESC
  } else if(key == CODED) { 
    if(connectedSerial) {
      if(!P.isFocus() && !I.isFocus() && !D.isFocus() && !targetAngle.isFocus()) { 
        if(keyCode == LEFT || keyCode == UP || keyCode == DOWN || keyCode == RIGHT) {        
          if(keyCode == LEFT) {  
            leftPressed = true;
            println("Left pressed");
          }
          if(keyCode == UP) {
            upPressed = true;
            println("Forward pressed");
          }
          if(keyCode == DOWN) {
            downPressed = true;
            println("Backward pressed");
          }
          if(keyCode == RIGHT) {
            rightPressed = true;
            println("Right pressed");
          }
          sendData = true;
        }
      }
    } else
      println("Establish a serial connection first!");
  }
}
void keyReleased() {
  if(connectedSerial) {
    if(!P.isFocus() && !I.isFocus() && !D.isFocus() && !targetAngle.isFocus()) { 
      if(keyCode == LEFT || keyCode == UP || keyCode == DOWN || keyCode == RIGHT) {        
        if(keyCode == LEFT) {
          leftPressed = false;
          println("Left released");
        }
        if(keyCode == UP) {
          upPressed = false;
          println("Up released");
        }
        if(keyCode == DOWN) {
          downPressed = false;
          println("Down released");
        }
        if(keyCode == RIGHT) {
          rightPressed = false;
          println("Right released");
        }
        sendData = true;
      }
    }
  } else
    println("Establish a serial connection first!");
}
void controlEvent(ControlEvent theEvent) {
  if(theEvent.isGroup()) {
    if(theEvent.group().name() == "COMPort")
      portNumber = int(theEvent.group().value()); //Since the list returns a float, we need to convert it to an int. For that we us the int() function.
  }
}
void Connect(int value) {     
  if(connectedSerial) // Disconnect existing connection
    Disconnect(0);
  if(portNumber != -1 && !connectedSerial) { // Check if com port and baudrate is set and if there is not already a connection established
    println("ConnectSerial");    
    try {
      serial = new Serial(this, Serial.list()[portNumber], 115200);
    } catch (Exception e) {
      //e.printStackTrace();
      println("Couldn't open serial port");
    }  
    if(serial != null) {      
      serial.bufferUntil('\n');
      connectedSerial = true;
      delay(100);
      serial.write("GP;"); // Get PID values
      delay(10);
      serial.write("IB;"); // Get IMU Data
    }
  } else if(portNumber == -1)
    println("Select COM Port first!");
  else if(connectedSerial)
    println("Already connected to a port!");
}
void Disconnect(int value) {    
  try {
    serial.write("IS;"); // Stop sending IMU values
    serial.stop();
    serial.clear(); // Empty the buffer
    connectedSerial = false;
    println("DisconnectSerial");
  } catch (Exception e) {
    //e.printStackTrace();
    println("Couldn't disconnect serial port");
  } 
}
