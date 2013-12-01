import processing.serial.*;
import controlP5.*;
import java.awt.event.*;

ControlP5 controlP5;

PFont font10, font25, font30;
Textfield P, I, D, targetAngle;
String stringP = "", stringI = "", stringD = "", stringTargetAngle = "";
String firmwareVer = "", eepromVer = "", mcu = "", voltage = "", minutes = "", seconds = "";

final boolean useDropDownLists = true; // Set if you want to use the dropdownlist or not
byte defaultComPort = 0; // You should change this as well if you do not want to use the dropdownlist

// Dropdown list
DropdownList dropdownList; // Define the variable ports as a Dropdownlist.
Serial serial; // Define the variable port as a Serial object.
int portNumber = -1; // The dropdown list will return a float value, which we will connvert into an int. We will use this int for that.

boolean connectedSerial, aborted;

boolean upPressed, downPressed, leftPressed, rightPressed, sendData;

String stringGyro, stringAcc, stringKalman;

// We will store 101 readings
float[] acc = new float[101];
float[] gyro = new float[101];
float[] kalman = new float[101];

boolean drawValues; // This is set to true whenever there is any new data

void setup() {
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

void draw() {
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
  text("Firmware: " + firmwareVer + " EEPROM: " + eepromVer + " MCU: " + mcu, 10, 300);
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
void abort() {
  if (connectedSerial) {
    serial.write("A;");
    println("Abort");
    aborted = true;
  } else
    println("Establish a serial connection first!");
}
void continueAbort() {
  if (connectedSerial) {
    serial.write("C");
    println("Continue");
    aborted = false;
  } else
    println("Establish a serial connection first!");
}
void submit() {
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
void clear() {
  P.clear();
  I.clear();
  D.clear();
  targetAngle.clear();
}
void restoreDefaults() {
  if (connectedSerial) {
    serial.write("CR;"); // Restore values
    println("RestoreDefaults");
    delay(10);
  } else
    println("Establish a serial connection first!");
}
void pairWithWiimote() {
  if (connectedSerial) {
    serial.write("CW;"); // Pair with Wiimote
    println("Pair with Wiimote");
    delay(10);
  } else
    println("Establish a serial connection first!");
}
void storeValues() {
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
void serialEvent(Serial serial) {
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
  } else if (input[0].equals("I") && input.length == 4) { // Info
    firmwareVer = input[1];
    eepromVer = input[2];
    mcu  = input[3];
  } else if (input[0].equals("R") && input.length == 3) { // Status response
    voltage  = input[1] + 'V';
    String runtime = input[2];
    minutes = str((int)floor(float(runtime)));
    seconds = str((int)(float(runtime)%1/(1.0/60.0)));
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
void keyPressed() {
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
void keyReleased() {
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
void controlEvent(ControlEvent theEvent) {
  if (theEvent.isGroup()) {
    if (theEvent.getGroup().getName() == dropdownList.getName())
      portNumber = int(theEvent.getGroup().getValue()); // Since the list returns a float, we need to convert it to an int. For that we us the int() function
  }
}
void connect() {
  if (connectedSerial) // Disconnect existing connection
    disconnect();
  if (portNumber != -1 && !connectedSerial) { // Check if com port and baudrate is set and if there is not already a connection established
    println("ConnectSerial");
    dropdownList.close();
    try {
      serial = new Serial(this, Serial.list()[portNumber], 115200);
    } catch (Exception e) {
      //e.printStackTrace();
      println("Couldn't open serial port");
    }
    if (serial != null) {
      serial.bufferUntil('\n');
      connectedSerial = true;
      delay(3000); // Wait bit - needed for the standard serial connection, as it resets the board
      serial.write("GP;"); // Get PID values
      delay(10);
      serial.write("GI;"); // Get info
      delay(10);
      serial.write("IB;"); // Start sending IMU values
      delay(10);
      serial.write("RB;"); // Start sending status report
    }
  } else if (portNumber == -1)
    println("Select COM Port first!");
  else if (connectedSerial)
    println("Already connected to a port!");
}

void disconnect() {
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
