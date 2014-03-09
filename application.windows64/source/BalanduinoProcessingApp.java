import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import controlP5.*; 
import java.awt.event.*; 
import java.awt.Image; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class BalanduinoProcessingApp extends PApplet {






ControlP5 controlP5;

PFont font10, font25, font30;
Textfield P, I, D, targetAngle, maxAngle, maxTurn, Qangle, Qbias, Rmeasure;
CheckBox checkBox;

String firmwareVer = "", eepromVer = "", mcu = "", voltage = "", minutes = "", seconds = "";

final boolean useDropDownLists = true; // Set if you want to use the dropdownlist or not
byte defaultComPort = 0; // You should change this as well if you do not want to use the dropdownlist

// Dropdown list
DropdownList dropdownList; // Define the variable ports as a Dropdownlist.
Serial serial; // Define the variable port as a Serial object.
int portNumber = -1; // The dropdown list will return a float value, which we will connvert into an int. We will use this int for that.

boolean connectedSerial;

boolean upPressed, downPressed, leftPressed, rightPressed, sendData;

String stringGyro, stringAcc, stringKalman;

// We will store 101 readings
float[] acc = new float[101];
float[] gyro = new float[101];
float[] kalman = new float[101];

boolean drawValues; // This is set to true whenever there is any new data

final int mainWidth = 337; // Width of the main control panel
final int graphWidth = 700; // Width of the graph

public void setup() {
  registerMethod("dispose", this); // Called automatically before shutting down

  frame.setTitle("Balanduino Processing App");
  frame.setIconImage((Image) loadImage("data/logo.png").getNative());

  controlP5 = new ControlP5(this);
  size(mainWidth + graphWidth, 510);

  font10 = loadFont("EuphemiaUCAS-Bold-10.vlw");
  font25 = loadFont("EuphemiaUCAS-Bold-25.vlw");
  font30 = loadFont("EuphemiaUCAS-Bold-30.vlw");

  /* For remote control */
  controlP5.addButton("up")
           .setPosition(mainWidth / 2 - 20, 70)
           .setSize(40, 20);

  controlP5.addButton("down")
           .setPosition(mainWidth / 2 - 20, 92)
           .setSize(40, 20);

  controlP5.addButton("left")
           .setPosition(mainWidth / 2 - 62, 92)
           .setSize(40, 20);

  controlP5.addButton("right")
           .setPosition(mainWidth / 2 + 22, 92)
           .setSize(40, 20);

  /* For setting the PID values etc. */
  P = controlP5.addTextfield("P")
               .setPosition(10, 165)
               .setSize(35, 20)
               .setFocus(true)
               .setInputFilter(ControlP5.FLOAT)
               .setAutoClear(false)
               .clear();

  I = controlP5.addTextfield("I")
               .setPosition(50, 165)
               .setSize(35, 20)
               .setInputFilter(ControlP5.FLOAT)
               .setAutoClear(false)
               .clear();

  D = controlP5.addTextfield("D")
               .setPosition(90, 165)
               .setSize(35, 20)
               .setInputFilter(ControlP5.FLOAT)
               .setAutoClear(false)
               .clear();

  targetAngle = controlP5.addTextfield("targetAngle")
                         .setPosition(130, 165)
                         .setSize(35, 20)
                         .setInputFilter(ControlP5.FLOAT)
                         .setAutoClear(false)
                         .clear();

  /* Settings */
  maxAngle = controlP5.addTextfield("maxAngle")
               .setPosition(10, 255)
               .setSize(40, 20)
               .setInputFilter(ControlP5.INTEGER)
               .setAutoClear(false)
               .clear();
               
  maxTurn = controlP5.addTextfield("maxTurn")
               .setPosition(55, 255)
               .setSize(35, 20)
               .setInputFilter(ControlP5.INTEGER)
               .setAutoClear(false)
               .clear();
               
  checkBox = controlP5.addCheckBox("BackToSpot")
                .setPosition(100, 255)
                .setColorForeground(color(120))
                .setColorActive(color(255))
                .setSize(20, 20)
                .addItem("Enable back to spot", 0);
                
  /* Kalman filte values */
  Qangle = controlP5.addTextfield("Qangle")
               .setPosition(10, 340)
               .setSize(40, 20)
               .setInputFilter(ControlP5.FLOAT)
               .setAutoClear(false)
               .clear();

  Qbias = controlP5.addTextfield("Qbias")
               .setPosition(55, 340)
               .setSize(40, 20)
               .setInputFilter(ControlP5.FLOAT)
               .setAutoClear(false)
               .clear();
               
  Rmeasure = controlP5.addTextfield("Rmeasure")
               .setPosition(100, 340)
               .setSize(40, 20)
               .setInputFilter(ControlP5.FLOAT)
               .setAutoClear(false)
               .clear();

  /* Buttons */
  controlP5.addButton("abort")
           .setPosition(10, 480)
           .setSize(40, 20);

  controlP5.addButton("continueAbort") // We have to call it something else, as continue is protected
           .setPosition(55, 480)
           .setSize(50, 20)
           .setCaptionLabel("continue");

  controlP5.addButton("submit")
           .setPosition(146, 455)
           .setSize(94, 20)
           .setCaptionLabel("Submit values");

  controlP5.addButton("restoreDefaults")
           .setPosition(146, 480)
           .setSize(94, 20)
           .setCaptionLabel("Restore defaults");

  controlP5.addButton("pairWithWiimote")
           .setPosition(245, 455)
           .setSize(82, 20)
           .setCaptionLabel("Pair with Wiimote");

  controlP5.addButton("pairWithPS4")
           .setPosition(245, 480)
           .setSize(82, 20)
           .setCaptionLabel("Pair with PS4");

  for (int i = 0; i < acc.length; i++) { // center all variables
    acc[i] = height / 2;
    gyro[i] = height / 2;
    kalman[i] = height / 2;
  }

  //println(Serial.list()); // Used for debugging
  if (useDropDownLists)
    initDropdownlist();
  else { // If useDropDownLists is false, it will connect automatically at startup
    portNumber = defaultComPort;
    connect();
  }

  drawGraph(); // Draw graph at startup
}

public void draw() {
  /* Draw Graph */
  if (connectedSerial && drawValues) {
    drawValues = false;
    drawGraph();
  }
  
  /* Remote contol */
  fill(0);
  stroke(0);
  rect(0, 0, mainWidth, height);
  fill(0, 102, 153);
  textSize(25);
  textFont(font25);
  textAlign(CENTER);
  text("Press buttons to steer", mainWidth / 2, 55);

  /* Set PID value etc. */
  fill(0, 102, 153);
  textSize(30);
  textFont(font30);
  textAlign(CENTER);
  text("Set PID Values:", mainWidth / 2, 155);
  text("Settings:", mainWidth / 2, 240);
  text("Kalman values:", mainWidth / 2, 330);

  fill(255, 255, 255);
  textSize(10);
  textFont(font10);
  textAlign(LEFT);
  text("Firmware: " + firmwareVer + " EEPROM: " + eepromVer + " MCU: " + mcu, 10, 410);
  String runtime;
  if (!minutes.isEmpty() && !seconds.isEmpty())
    runtime =  minutes + " min " + seconds + " sec";
  else
    runtime = "";
  text("Battery level: " + voltage + " Runtime: " + runtime, 10, 430);

  if (sendData) { // Data is send as x,y-coordinates
    if (upPressed) {
      if (leftPressed)
        serial.write("CJ,-0.7,0.7;"); // Forward left
      else if (rightPressed)
        serial.write("CJ,0.7,0.7;"); // Forward right
      else
        serial.write("CJ,0,0.7;"); // Forward
    }
    else if (downPressed) {
      if (leftPressed)
        serial.write("CJ,-0.7,-0.7;"); // Backward left
      else if (rightPressed)
        serial.write("CJ,0.7,-0.7;"); // Backward right
      else
        serial.write("CJ,0,-0.7;"); // Backward
    }
    else if (leftPressed)
      serial.write("CJ,-0.7,0;"); // Left
    else if (rightPressed)
      serial.write("CJ,0.7,0;"); // Right
    else {
      serial.write("CS;");
      println("Stop");
    }
    sendData = false;
  }
}

public void abort() {
  if (connectedSerial) {
    serial.write("A;");
    println("Abort");
  } else
    println("Establish a serial connection first!");
}

public void continueAbort() {
  if (connectedSerial) {
    serial.write("C");
    println("Continue");
  } else
    println("Establish a serial connection first!");
}

public void submit() {
  if (connectedSerial) {
    println("PID values: " + P.getText() + " " + I.getText() + " " + D.getText() +  " TargetAnlge: " + targetAngle.getText());
    if (!P.getText().isEmpty()) {
      println("Send P value");
      serial.write("SP," + P.getText() + ';');
      delay(10);
    }
    if (!I.getText().isEmpty()) {
      println("Send I value");
      serial.write("SI," + I.getText() + ';');
      delay(10);
    }
    if (!D.getText().isEmpty()) {
      println("Send D value");
      serial.write("SD," + D.getText() + ';');
      delay(10);
    }
    if (!targetAngle.getText().isEmpty()) {
      println("Send target angle");
      serial.write("ST," + targetAngle.getText() + ';');
      delay(10);
    }
    
    println("Kalman values: " + Qangle.getText() + " " + Qbias.getText() + " " + Rmeasure.getValue());
    if (!Qangle.getText().isEmpty() && !Qbias.getText().isEmpty() && !Rmeasure.getText().isEmpty()) {
      println("Send Kalman values");
      serial.write("SK," + Qangle.getText() + ',' + Qbias.getText() + ',' + Rmeasure.getText() + ';');
      delay(10);
    }
    
    println("Settings: " + maxAngle.getText() + " " + maxTurn.getText() + " " + checkBox.getArrayValue(0));
    if (!maxAngle.getText().isEmpty()) {
      println("Send max angle");
      serial.write("SA," + maxAngle.getText() + ';');
      delay(10);
    }
    if (!maxTurn.getText().isEmpty()) {
      println("Send turning angle");
      serial.write("SU," + maxTurn.getText() + ';');
      delay(10);
    }
    println("Send Back to spot");
    serial.write("SB," + (checkBox.getArrayValue(0) == 1 ? '1' : '0') + ';');
    delay(10);

    serial.write("GP;"); // Get PID values
    delay(50);
    serial.write("GK;"); // Get Kalman values
    delay(50);
    serial.write("GS;"); // Get settings values
    delay(10);
  } else
    println("Establish a serial connection first!");
}

public void restoreDefaults() {
  if (connectedSerial) {
    serial.write("CR;"); // Restore values
    println("RestoreDefaults");
    delay(10);
  } else
    println("Establish a serial connection first!");
}

public void pairWithPS4() {
  if (connectedSerial) {
    serial.write("CPP;"); // Pair with PS4
    println("Pair with PS4 controller");
    delay(10);
  } else
    println("Establish a serial connection first!");
}

public void pairWithWiimote() {
  if (connectedSerial) {
    serial.write("CPW;"); // Pair with Wiimote
    println("Pair with Wiimote");
    delay(10);
  } else
    println("Establish a serial connection first!");
}

public void serialEvent(Serial serial) {
  String[] input = trim(split(serial.readString(), ','));

  /*print("Length: " + input.length + " "); // Uncomment for debugging
  for (int i = 0; i<input.length;i++)
    print("Number: " + i + " Input: " + input[i] + " ");
  println();*/

  if (input[0].equals("P") && input.length == 5) { // PID values
    P.setText(input[1]);
    I.setText(input[2]);
    D.setText(input[3]);
    targetAngle.setText(input[4]);
  } else if (input[0].equals("K") && input.length == 4) { // Kalman values
    Qangle.setText(input[1]);
    Qbias.setText(input[2]);
    Rmeasure.setText(input[3]);
  } else if (input[0].equals("S") && input.length == 4) { // Settings
    checkBox.getItem(0).setValue(input[1].equals("1") ? 1 : 0);
    maxAngle.setText(input[2]);
    maxTurn.setText(input[3]);
  } else if (input[0].equals("I") && input.length == 4) { // Info
    firmwareVer = input[1];
    eepromVer = input[2];
    mcu  = input[3];
  } else if (input[0].equals("R") && input.length == 3) { // Status response
    voltage  = input[1] + 'V';
    String runtime = input[2];
    minutes = str((int)floor(PApplet.parseFloat(runtime)));
    seconds = str((int)(PApplet.parseFloat(runtime) % 1 / (1.0f / 60.0f)));
  } else if (input[0].equals("V") && input.length == 4) { // IMU data
    stringAcc = input[1];
    stringGyro = input[2];
    stringKalman = input[3];
  } else if (input[0].equals("PC"))
    println("Now enable discovery of your device");

  serial.clear();  // Empty the buffer
  drawValues = true; // Draw the graph
}

public void keyPressed() {
  if (key == TAB) { //'\t'
    if (P.isFocus()) {
      P.setFocus(false);
      I.setFocus(true);
    } else if (I.isFocus()) {
      I.setFocus(false);
      D.setFocus(true);
    } else if (D.isFocus()) {
      D.setFocus(false);
      targetAngle.setFocus(true);
    } else if (targetAngle.isFocus()) {
      targetAngle.setFocus(false);
      P.setFocus(true);
    }
    else if (maxAngle.isFocus()) {
      maxAngle.setFocus(false);
      maxTurn.setFocus(true);
    } else if (maxTurn.isFocus()) {
      maxTurn.setFocus(false);
      maxAngle.setFocus(true);
    }
    else if (Qangle.isFocus()) {
      Qangle.setFocus(false);
      Qbias.setFocus(true);
    } else if (Qbias.isFocus()) {
      Qbias.setFocus(false);
      Rmeasure.setFocus(true);
    } else if (Rmeasure.isFocus()) {
      Rmeasure.setFocus(false);
      Qangle.setFocus(true);
    }
    else
      P.setFocus(true);
  }
  else if (key == ENTER) { // '\n'
    if (connectedSerial)
      submit(); // If we are connected, send the values
    else
      connect(); // If not try to connect
  }
  else if (key == ESC) {
    disconnect(); // Disconnect serial connection
    key = 0; // Disable Processing from quiting when pressing ESC
  } else if (key == CODED)
    handleButtons(keyCode, true);
}

public void handleButtons(int button, boolean pressed) {
  if (connectedSerial) {
    if (!P.isFocus() && !I.isFocus() && !D.isFocus() && !targetAngle.isFocus() && !maxAngle.isFocus() && !maxTurn.isFocus() && !Qangle.isFocus() && !Qbias.isFocus() && !Rmeasure.isFocus()) {
      if (button == LEFT || button == UP || button == DOWN || button == RIGHT) {
        if (button == LEFT) {
          leftPressed = pressed;
          println("Left " + (pressed ? "pressed" : "released"));
        } else if (button == UP) {
          upPressed = pressed;
          println("Up " + (pressed ? "pressed" : "released"));
        } else if (button == DOWN) {
          downPressed = pressed;
          println("Down " + (pressed ? "pressed" : "released"));
        } else if (button == RIGHT) {
          rightPressed = pressed;
          println("Right " + (pressed ? "pressed" : "released"));
        }
        sendData = true;
      }
    }
  } else
    println("Establish a serial connection first!");
}

public void keyReleased() {
  if (key == CODED)
    handleButtons(keyCode, false);
}

public void controlEvent(ControlEvent theEvent) {
  if (theEvent.isGroup()) {
    if (theEvent.getGroup().getName() == dropdownList.getName())
      portNumber = PApplet.parseInt(theEvent.getGroup().getValue()); // Since the list returns a float, we need to convert it to an int. For that we us the int() function
  }
}

public void connect() {
  disconnect(); // Disconnect any existing connection
  if (portNumber != -1) { // Check if com port and baudrate is set and if there is not already a connection established
    println("ConnectSerial");
    dropdownList.close();
    try {
      serial = new Serial(this, Serial.list()[portNumber], 115200);
    } catch (Exception e) {
      println("Couldn't open serial port");
      e.printStackTrace();
    }
    if (serial != null) {
      serial.bufferUntil('\n');
      connectedSerial = true;
      delay(3000); // Wait bit - needed for the standard serial connection, as it resets the board
      serial.write("GP;"); // Get PID values
      delay(50);
      serial.write("GK;"); // Get Kalman values
      delay(50);
      serial.write("GS;"); // Get settings values
      delay(50);
      serial.write("GI;"); // Get info
      delay(50);
      serial.write("IB;"); // Start sending IMU values
      delay(50);
      serial.write("RB;"); // Start sending status report
    }
  } else if (portNumber == -1)
    println("Select COM Port first!");
  else if (connectedSerial)
    println("Already connected to a port!");
}

public void dispose() { // Called automatically before shutting down
  disconnect();
}

public void disconnect() {
  if (!connectedSerial)
    return;
  try {
    serial.write("IS;"); // Stop sending IMU values
    delay(10);
    serial.write("RS;"); // Stop sending status report
    delay(500);
    serial.stop();
    serial.clear(); // Empty the buffer
    connectedSerial = false;
    println("DisconnectSerial");
  } catch (Exception e) {
    //e.printStackTrace();
    println("Couldn't disconnect serial port");
  }
}
final int minIMUAngle = 0, maxIMUAngle = 360;

public void drawGraph() {
  background(255); // Set background to white

  stroke(200); // Grey
  for (int i = 0; i < gyro.length; i++) { // Draw graph paper
    line(mainWidth + i * 10, 0, mainWidth + i * 10, height);
    line(mainWidth, i * 10, width, i * 10);
  }

  stroke(0); // Black
  for (int i = 1; i <= 3; i++)
    line(mainWidth, height / 4 * i, width, height / 4 * i); // Draw lines indicating 90 deg, 180 deg, and 270 deg

  if (stringGyro != null)
    gyro[gyro.length - 1] = map(PApplet.parseFloat(trim(stringGyro)), minIMUAngle, maxIMUAngle, 0, height); // Convert to an float and map to the screen height, then save in buffer
  if (stringAcc != null)
    acc[acc.length - 1] = map(PApplet.parseFloat(trim(stringAcc)), minIMUAngle, maxIMUAngle, 0, height); // Convert to an float and map to the screen height, then save in buffer
  if (stringKalman != null)
    kalman[kalman.length - 1] = map(PApplet.parseFloat(trim(stringKalman)), minIMUAngle, maxIMUAngle, 0, height); // Convert to an float and map to the screen height, then save in buffer

  noFill();

  // Draw acceleromter x-axis
  stroke(255, 0, 0); // Red
  beginShape();
  for (int i = 0; i < acc.length; i++)
    vertex(i * graphWidth / (acc.length - 1) + mainWidth, height - acc[i]);
  endShape();

  // Draw gyro x-axis
  stroke(0, 255, 0); // Green
  beginShape();
  for (int i = 0; i < gyro.length; i++)
    vertex(i * graphWidth / (gyro.length - 1) + mainWidth, height - gyro[i]);
  endShape();

  // Draw kalman filter x-axis
  stroke(0, 0, 255); // Blue
  beginShape();
  for (int i = 0; i < kalman.length; i++)
    vertex(i * graphWidth / (kalman.length - 1) + mainWidth, height - kalman[i]);
  endShape();

  for (int i = 1; i < acc.length; i++) { // Put all data one array back
    acc[i - 1] = acc[i];
    gyro[i - 1] = gyro[i];
    kalman[i - 1] = kalman[i];
  }
}
public void initDropdownlist() {
  dropdownList = controlP5.addDropdownList("SerialPort"); // Make a dropdown list with all serial ports

  dropdownList.setPosition(10, 20);
  dropdownList.setSize(210, 200);
  dropdownList.setCaptionLabel("Select serial port"); // Set the lable of the bar when nothing is selected

  dropdownList.setBackgroundColor(color(200)); // Set the background color of the line between values
  dropdownList.setItemHeight(20); // Set the height of each item when the list is opened
  dropdownList.setBarHeight(15); // Set the height of the bar itself

  dropdownList.getCaptionLabel().getStyle().marginTop = 3; // Set the top margin of the lable
  dropdownList.getCaptionLabel().getStyle().marginLeft = 3; // Set the left margin of the lable

  dropdownList.setColorBackground(color(60));
  dropdownList.setColorActive(color(255, 128));

  // Now add the ports to the list, we use a for loop for that
  for (int i = 0; i < Serial.list().length; i++) {
    if (Serial.list()[i].indexOf("/dev/cu.") != -1)
      continue; // Do not display /dev/cu.* devices
    dropdownList.addItem(Serial.list()[i], i); // This is the line doing the actual adding of items, we use the current loop we are in to determine what place in the char array to access and what item number to add it as
    if (Serial.list()[i].indexOf("Balanduino") != -1) // Check for the "Balanduino" substring
      dropdownList.setValue(i); // Automaticly select the Balanduino balancing robot on Mac OS X and Linux
  }

  addMouseWheelListener(new MouseWheelListener() { // Add a mousewheel listener to scroll the dropdown list
    public void mouseWheelMoved(MouseWheelEvent mwe) {
      dropdownList.scroll(mwe.getWheelRotation() > 0 ? 1 : 0); // Scroll the dropdownlist using the mousewheel
    }
  });

  controlP5.addButton("connect")
           .setPosition(225, 3)
           .setSize(45, 15);

  controlP5.addButton("disconnect")
           .setPosition(275, 3)
           .setSize(52, 15);
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "BalanduinoProcessingApp" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
