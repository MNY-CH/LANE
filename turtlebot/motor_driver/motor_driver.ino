#include <RC100.h>
#include <DynamixelSDK.h>

#define LEFT_DXL  1
#define RIGHT_DXL 2

#define BAUDRATE 1000000
#define DEVICENAME ""
#define PROTOCOL_VERSION 2.0

#define ADDR_TORQUE_ENABLE 64
#define ADDR_GOAL_VELOCITY 104

#define LEN_TORQUE_ENABLE 1
#define LEN_GOAL_VELOCITY 4

#define ON  1
#define OFF 0

#define VELOCITY 10

dynamixel::PortHandler *portHandler;
dynamixel::PacketHandler *packetHandler;

dynamixel::GroupSyncWrite *groupSyncWrite;

bool dxl_addparam_result = false;
int dxl_comm_result = COMM_TX_FAIL;
uint8_t dxl_error = 0;

int vel[2] = {0, 0};
int const_vel = 200;

RC100 Controller;
int RcvData = 0;
String InputString = "";
boolean StringComplete = 0;
boolean SetValidData = 0; 

void setup() 
{
  Serial.begin(57600);
  pinMode(13, OUTPUT);
  Controller.begin(1);

  portHandler = dynamixel::PortHandler::getPortHandler(DEVICENAME);
  packetHandler = dynamixel::PacketHandler::getPacketHandler(PROTOCOL_VERSION);
  groupSyncWrite = new dynamixel::GroupSyncWrite(portHandler, packetHandler, ADDR_GOAL_VELOCITY, LEN_GOAL_VELOCITY);

  portHandler -> openPort();
  portHandler->setBaudRate(BAUDRATE);

  dxl_comm_result = packetHandler->write1ByteTxRx(portHandler, LEFT_DXL, ADDR_TORQUE_ENABLE, ON, &dxl_error);
  packetHandler->getTxRxResult(dxl_comm_result);

  dxl_comm_result = packetHandler->write1ByteTxRx(portHandler, RIGHT_DXL, ADDR_TORQUE_ENABLE, ON, &dxl_error);
  packetHandler->getTxRxResult(dxl_comm_result);
}

void loop() 
{ 
    Serial.flush();
    delay(100);
  if(Serial.available()){
    digitalWrite(13, HIGH);
    String inString = Serial.readStringUntil('\n');
    Serial.println(inString);
    splitAndRun(inString, ' ');
 }
   
   digitalWrite(13, LOW);
   
}

void controlMotor(uint16_t* left_wheel_value, uint16_t* right_wheel_value)
{
  //bool dxl_addparam_result;
  //int dxl_comm_result;

  groupSyncWrite->addParam(LEFT_DXL, (uint8_t*)&left_wheel_value);
  groupSyncWrite->addParam(RIGHT_DXL, (uint8_t*)&right_wheel_value);

  groupSyncWrite->txPacket();

  groupSyncWrite->clearParam();
}

void splitAndRun(String sData, char cSeparator) 
{  
  int nGetIndex = 0 ;
  int left = 0, right = 0; 
  nGetIndex = sData.indexOf(cSeparator);
  left = sData.substring(0, nGetIndex).toInt();
  right = sData.substring(nGetIndex + 1).toInt();
  controlMotor((uint16_t*)left, (uint16_t*)right);
}
