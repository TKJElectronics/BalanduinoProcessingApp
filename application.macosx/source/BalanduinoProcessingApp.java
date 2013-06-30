import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import controlP5.*; 
import java.awt.event.*; 

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

PFont font10;
PFont font25;
PFont font30;

Textfield P;
Textfield I;
Textfield D;
Textfield targetAngle;

String stringP = "";
String stringI = "";
String stringD = "";
String stringTargetAngle = "";

String firmwareVer = "";
String mcu = "";
String voltage = "";
String minutes = "";
String seconds = "";

final boolean useDropDownLists = true; // Set if you want to use the dropdownlist or not
byte defaultComPort = 0;

// Dropdown list
DropdownList dropdownList; // Define the variable ports as a Dropdownlist.
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

// We will store 101 readings
float[] acc = new float[101];
float[] gyro = new float[101];
float[] kalman = new float[101];

boolean drawValues; // This is set to true whenever there is any new data

public void setup() {
  controlP5 = new ControlP5(this);
  size(937, 370);
  
  font10 = loadFont("EuphemiaUCAS-Bold-10.vlw");
  font25 = loadFont("EuphemiaUCAS-Bold-25.vlw");
  font30 = loadFont("EuphemiaUCAS-Bold-30.vlw");
  
  /* For remote control */
  controlP5.addButton("up")
           .setPosition(337/2-20, 70)
           .setSize(40, 20);
           
  controlP5.addButton("down")
           .setPosition(337/2-20, 92)
           .setSize(40, 20);
           
  controlP5.addButton("left")
           .setPosition(337/2-62, 92)
           .setSize(40, 20);
           
  controlP5.addButton("right")
           .setPosition(337/2+22, 92)
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
                         
  controlP5.addButton("submit")
           .setPosition(202, 165)
           .setSize(60, 20);
           
  controlP5.addButton("clear")
           .setPosition(267, 165)
           .setSize(60, 20);
           
  controlP5.addButton("abort")
           .setPosition(10, 340)
           .setSize(40, 20);
           
  controlP5.addButton("continueAbort") // We have to call it something else, as continue is protected
           .setPosition(55, 340)
           .setSize(50, 20)
           .setCaptionLabel("continue");
           
  controlP5.addButton("storeValues")
           .setPosition(175, 340)
           .setSize(65, 20)
           .setCaptionLabel("Store values");
           
  controlP5.addButton("pairWithWiimote")
           .setPosition(245, 315)
           .setSize(82, 20)
           .setCaptionLabel("Pair with Wiimote");
           
  controlP5.addButton("restoreDefaults")
           .setPosition(245, 340)
           .setSize(82, 20)
           .setCaptionLabel("Restore defaults");
           
  for (int i=0;i<acc.length;i++) { // center all variables    
    acc[i] = height/2;
    gyro[i] = height/2;
    kalman[i] = height/2;
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
  rect(0, 0, 337, height);
  fill(0, 102, 153);
  textSize(25);
  textFont(font25);
  textAlign(CENTER);
  text("Press buttons to steer", 337/2, 55);

  /* Set PID value etc. */
  fill(0, 102, 153);
  textSize(30);
  textFont(font30);
  textAlign(CENTER);
  text("Set PID Values:", 337/2, 155);
  text("Current PID Values:", 337/2, 250);

  fill(255, 255, 255);
  textSize(10);
  textFont(font10);
  textAlign(LEFT);
  text("P: " + stringP + " I: " + stringI +  " D: " + stringD + " TargetAngle: " + stringTargetAngle, 10, 280);
  text("Firmware: " + firmwareVer + " MCU: " + mcu, 10, 300);
  String runtime;
  if (!minutes.isEmpty() && !seconds.isEmpty())
    runtime =  minutes + " min " + seconds + " sec";
  else
    runtime = "";
  text("Battery level: " + voltage + " Runtime: " + runtime, 10, 320);

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
    aborted = true;
  } else
    println("Establish a serial connection first!");
}
public void continueAbort() {
  if (connectedSerial) {
    serial.write("C");
    println("Continue");
    aborted = false;
  } else
    println("Establish a serial connection first!");
}
public void submit() {
  if (connectedSerial) {    
    println("PID values: " + P.getText() + " " + I.getText() + " " + D.getText() +  " TargetAnlge: " + targetAngle.getText());
    
    if (!P.getText().equals(stringP) && !P.getText().isEmpty()) {
      println("Send P value");
      serial.write("SP," + P.getText() + ';');
      delay(10);
    }
    if (!I.getText().equals(stringI) && !I.getText().isEmpty()) {
      println("Send I value");
      serial.write("SI," + I.getText() + ';');
      delay(10);
    }
    if (!D.getText().equals(stringD) && !D.getText().isEmpty()) {
      println("Send D value");
      serial.write("SD," + D.getText() + ';');
      delay(10);
    }
    if (!targetAngle.getText().equals(stringTargetAngle) && !targetAngle.getText().isEmpty()) {
      println("Send target angle");
      serial.write("ST," + targetAngle.getText() + ';');
      delay(10);
    }
    serial.write("GP;"); // Get PID values
  } else
    println("Establish a serial connection first!");
}
public void clear() {
  P.clear();
  I.clear();
  D.clear();
  targetAngle.clear();
}
public void restoreDefaults() {
  if (connectedSerial) {
    serial.write("CR;"); // Restore values
    println("RestoreDefaults");
    delay(10);
  } else
    println("Establish a serial connection first!");
}
public void pairWithWiimote() {
  if (connectedSerial) {
    serial.write("CW;"); // Pair with Wiimote
    println("Pair with Wiimote");
    delay(10);
  } else
    println("Establish a serial connection first!");
}
public void storeValues() {
  // Don't set the text if the string is empty or it will throw an exception
  if (stringP != null)
    P.setText(stringP);
  if (stringI != null)
    I.setText(stringI);
  if (stringD != null)
    D.setText(stringD);
  if (stringTargetAngle != null)
    targetAngle.setText(stringTargetAngle);
}
public void serialEvent(Serial serial) {
  String[] input = trim(split(serial.readString(), ','));
  
  /*print("Length: " + input.length + " "); // Uncomment for debugging
  for (int i = 0; i<input.length;i++)
    print("Number: " + i + " Input: " + input[i] + " ");
  println();*/

  if (input[0].equals("P") && input.length == 5) { // PID values
    stringP = input[1];
    stringI = input[2];
    stringD = input[3];
    stringTargetAngle = input[4];
    
    // Set the text fields if they are empty
    if (P.getText().isEmpty())
      P.setText(stringP);
    if (I.getText().isEmpty())
      I.setText(stringI);
    if (D.getText().isEmpty())
      D.setText(stringD);
    if (targetAngle.getText().isEmpty())
      targetAngle.setText(stringTargetAngle);
  } else if (input[0].equals("I") && input.length == 5) { // Info
    firmwareVer = input[1];
    mcu  = input[2];
    voltage  = input[3];
    String runtime = input[4];
    minutes = str((int)floor(PApplet.parseFloat(runtime)));
    seconds = str((int)(PApplet.parseFloat(runtime)%1/(1.0f/60.0f)));
  } else if (input[0].equals("V") && input.length == 4) { // IMU data
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
public void keyPressed() {
  if (key == 's' || key == 'S')
    storeValues();
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
    } else
      P.setFocus(true);
  }
  else if (key == ENTER) { // '\n'
    if (connectedSerial)
      submit(); // If we are connected, send the values
    else
      connect(); // If not try to connect
  }
  else if (key == ESC) {
    if (aborted)
      continueAbort();
    else
      abort();      
    key = 0; // Disable Processing from quiting when pressing ESC
  } else if (key == CODED) { 
    if (connectedSerial) {
      if (!P.isFocus() && !I.isFocus() && !D.isFocus() && !targetAngle.isFocus()) { 
        if (keyCode == LEFT || keyCode == UP || keyCode == DOWN || keyCode == RIGHT) {        
          if (keyCode == LEFT) {  
            leftPressed = true;
            println("Left pressed");
          }
          if (keyCode == UP) {
            upPressed = true;
            println("Forward pressed");
          }
          if (keyCode == DOWN) {
            downPressed = true;
            println("Backward pressed");
          }
          if (keyCode == RIGHT) {
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
public void keyReleased() {
  if (connectedSerial) {
    if (!P.isFocus() && !I.isFocus() && !D.isFocus() && !targetAngle.isFocus()) { 
      if (keyCode == LEFT || keyCode == UP || keyCode == DOWN || keyCode == RIGHT) {        
        if (keyCode == LEFT) {
          leftPressed = false;
          println("Left released");
        }
        if (keyCode == UP) {
          upPressed = false;
          println("Up released");
        }
        if (keyCode == DOWN) {
          downPressed = false;
          println("Down released");
        }
        if (keyCode == RIGHT) {
          rightPressed = false;
          println("Right released");
        }
        sendData = true;
      }
    }
  } else
    println("Establish a serial connection first!");
}
public void controlEvent(ControlEvent theEvent) {
  if (theEvent.isGroup()) {
    if (theEvent.getGroup().getName() == dropdownList.getName())
      portNumber = PApplet.parseInt(theEvent.getGroup().getValue()); // Since the list returns a float, we need to convert it to an int. For that we us the int() function
  }
}
public void connect() {     
  if (connectedSerial) // Disconnect existing connection
    disconnect();
  if (portNumber != -1 && !connectedSerial) { // Check if com port and baudrate is set and if there is not already a connection established
    println("ConnectSerial");    
    try {
      serial = new Serial(this, Serial.list()[portNumber], 115200);
    } catch (Exception e) {
      //e.printStackTrace();
      println("Couldn't open serial port");
    }  
    if (serial != null) {      
      serial.bufferUntil('\n');
      connectedSerial = true;
      delay(100);
      serial.write("GP;"); // Get PID values
      delay(10);
      serial.write("GI;"); // Get info
      delay(10);
      serial.write("IB;"); // Get IMU Data
    }
  } else if (portNumber == -1)
    println("Select COM Port first!");
  else if (connectedSerial)
    println("Already connected to a port!");
}

public void disconnect() {    
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
final int maxAngle = 360;
final int minAngle = 0;

public void drawGraph() {
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
    gyro[gyro.length-1] = map(PApplet.parseFloat(trim(stringGyro)), minAngle, maxAngle, 0, height); // Convert to an float and map to the screen height, then save in buffer
  if (stringAcc != null)
    acc[acc.length-1] = map(PApplet.parseFloat(trim(stringAcc)), minAngle, maxAngle, 0, height); // Convert to an float and map to the screen height, then save in buffer
  if (stringKalman != null)
    kalman[kalman.length-1] = map(PApplet.parseFloat(trim(stringKalman)), minAngle, maxAngle, 0, height); // Convert to an float and map to the screen height, then save in buffer
  
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
  
  // Now well add the ports to the list, we use a for loop for that
  for (int i=0; i<Serial.list().length; i++) {
    dropdownList.addItem(Serial.list()[i], i); // This is the line doing the actual adding of items, we use the current loop we are in to determine what place in the char array to access and what item number to add it as
    if (Serial.list()[i].indexOf("Balanduino") != -1 && dropdownList.getValue() == 0) // Check for the "Balanduino" substring and make sure it is not already set
      dropdownList.setValue(i); // Automaticly select the Balanduino balancing robot on Mac OS X and Linux
  }
  
  addMouseWheelListener(new MouseWheelListener() { // Add a mousewheel listener to scroll the dropdown list
    public void mouseWheelMoved(MouseWheelEvent mwe) {
      dropdownList.scroll(mwe.getWheelRotation() > 0 ? 1 : 0); // Scroll the dropdownlist using the mousewheel
  }});

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
