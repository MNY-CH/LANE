#sudo python3, cv2
#https://www.raspberrypi.org/forums/viewtopic.php?t=277949

import cv2
from bluetooth import *
from time import sleep
from picamera import PiCamera

import socket
import serial as seri
import numpy as np
import time
import argparse

from io import BytesIO
from time import sleep
from picamera import PiCamera
from PIL import Image
import numpy as np

def softmax(x):
    f_x = np.exp(x) / np.sum(np.exp(x))
    return f_x

LOG_FLAG = False
COMMENT = "OFF"
def log(message):
    if(LOG_FLAG):
        print(message)

def receiveMsg():
    uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"
    stream = BytesIO()
    camera = PiCamera(resolution=(1280, 720))
    
    lane_flag = False
    
    # RFCOMM 포트를 통해 데이터 통신을 하기 위한 준비    
    server_sock=BluetoothSocket( RFCOMM )
    server_sock.bind(('',PORT_ANY))
    server_sock.listen(1)

    port = server_sock.getsockname()[1]

    # 블루투스 서비스를 Advertise
    advertise_service( server_sock, "BtChat",
            service_id = uuid,
            service_classes = [ uuid, SERIAL_PORT_CLASS ],
            profiles = [ SERIAL_PORT_PROFILE ] )
    
    distance = {4 : 7, 3 : 10, 2 : 14, 1 : 20}
    log("[Bluetooth] Waiting for connection : channel " + str(port))
    # 클라이언트가 연결될 때까지 대기
    client_sock, client_info = server_sock.accept()
    log('[Bluetooth] accepted')
    ser = seri.Serial('/dev/ttyACM0', 57600, timeout=1)
    while True:          
        #log("[Bluetooth] : Accepted connection from " + str(client_info))
        try:
            data = client_sock.recv(1024)
            if len(data) == 0: break
            log("[Bluetooth] received :" +  str(data)[2:-1])

            start_time = time.time()
            tmp_time = time.time()

            #Taek Picture
            camera.capture(stream, format='jpeg')
            stream.seek(0)
            log("Capture!")

            #TAKE PICTURE time
            stop_time = time.time()
            if(COMMENT == 'ON'):
                print('time: {:.3f}ms'.format((stop_time - tmp_time) * 1000) +"<br>")
            tmp_time = time.time()
            
            #Read image
            img = np.array(Image.open(stream))

            #Crop Ratio
            sli_height = int(img.shape[0] / width);
            sli_width = int(img.shape[1] / height);

            #Resize Image
            resize_Img = cv2.resize(img,( width * sli_width, height * sli_height))

            crack = 0
            
            #RESIZE TIME
            stop_time = time.time()
            if(COMMENT == 'ON'):
                print('time: {:.3f}ms'.format((stop_time - tmp_time) * 1000) +"<br>")
                
            tmp_time = time.time()
            result_array = []
            #Loop -> sli_width * sli_height
            loop_index = [1, sli_width]
            loop_height_index = [sli_height - 1]
            for i in loop_index:
                for j in loop_height_index:
                
                    #Get X location
                    x1 = (i - 1) * width
                    x2 = i * width

                    #Get Y location
                    y1 = (j - 1) * height
                    y2 = j * height

                    #Set input data
                    input_img = resize_Img[y1 : y2 , x1 : x2].copy()

                    #new_img = cv2.resize(input_img, (224, 224))
                    new_img = input_img.astype(np.float32)
                    new_img /= 255.
                    new_img = new_img.transpose(2,0,1)

                    #Use for debug
                    #print(input_data.shape)

                    #Get Result
                    interpreter.set_tensor(inputdets[0]['index'], [new_img])
                    interpreter.invoke()
                    result = interpreter.get_tensor(outputdets[0]['index'])
                    if(COMMENT == 'ON'):
                        print(softmax(result[0]))
                    
                    result_array.append(np.argmax(softmax(result[0])))
            
            if(lane_flag):
                if(result_array[0] == 1 and result_array[1] == 1):
                    if(COMMENT == 'ON'):
                        print("send!")
                    client_sock.send("1")
                    lane_flag = False
                else:
                    lane_flag = False
            else:
                if(result_array[0] == 1 and result_array[1] == 1):
                    if(COMMENT == 'ON'):
                        print("Flag!")
                    lane_flag = True
                else:
                    lane_flag = False
                
            #MODEL time
            stop_time = time.time()
            if(COMMENT == 'ON'):
                print('time: {:.3f}ms'.format((stop_time - tmp_time) * 1000) +"<br>")
            tmp_time = time.time()
            
            serial_data = str(data)[2:-1].split(" ")
            if(COMMENT == 'ON'):
                print(serial_data[0] + "/" +serial_data[1])
            
            ser.write(str.encode(serial_data[0] + " " +  serial_data[1] + "\n"));
            
            
            stream = BytesIO()
            #Get time
            stop_time = time.time()
            if(COMMENT == 'ON'):
                print('time: {:.3f}ms'.format((stop_time - start_time) * 1000) +"<br>")
#             client_sock.send(data)
            
        except IOError:
            ser.write(str.encode("0 0" + "\n"));
            log("[Bluetooth] disconnected")
            client_sock.close()
            server_sock.close()
            log("[Bluetooth] exit")
            break

        except KeyboardInterrupt:
            ser.write(str.encode("0 0" + "\n"));
            log("[Bluetooth] disconnected")
            client_sock.close()
            server_sock.close()
            log("[Bluetooth] exit")
            break

parser = argparse.ArgumentParser()
parser.add_argument(
	'-m',
	'--model_file',
	default='/home/pi/Test/mymodel.tflite',
	help='.tflite model to be executed')
args = parser.parse_args()
        
    
#Model input size
width = 224
height = 224

#Expected Accuracy
acc = 0.9

#Load Tensorflow Lite
import tflite_runtime.interpreter as tflite
interpreter = tflite.Interpreter(model_path=args.model_file)
interpreter.allocate_tensors()
inputdets = interpreter.get_input_details()
outputdets = interpreter.get_output_details()

speed = 7.5
label = ['lane', 'none']

receiveMsg()