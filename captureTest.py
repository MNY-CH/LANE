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

width = 224
height = 224

stream = BytesIO()
camera = PiCamera(resolution=(1280, 720))
camera.capture(stream, format='jpeg')
img = np.array(Image.open(stream))
#img = img.transpose(1, 0, 2)
sli_height = int(img.shape[0] / width) + 1;
sli_width = int(img.shape[1] / height) + 1;

print(img.shape)
#Resize Image
resize_Img = cv2.resize(img,( width * sli_width, height * sli_height))

print(resize_Img.shape)
print((sli_width, sli_height))
loop_index = [1, sli_width]
for i in loop_index:
    for j in range(1, sli_height + 1):
        print(str(i - 1) + "/" + str(j - 1))
        #Get X location
        x1 = (i - 1) * width
        x2 = i * width

        #Get Y location
        y1 = (j - 1) * height
        y2 = j * height

        #Set input data
        input_img = resize_Img[y1 : y2 , x1 : x2].copy()
        cv2.imwrite("Test2/frame%d_%d.jpg" % (i - 1, j - 1), input_img) 
cv2.imwrite("Test2/frame.jpg", resize_Img) 
