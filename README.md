# OpenCV In-air Hand-writting method for English & Chinese Letter based on RGB Camera

# Algorithm
1. Using skin color segmentation to extract hand area and exclude close color area and arm.
2. Tracking finger movement through opencv and using number of fingers as sign of input state.
3. Processing the recorded track and using OCR module to recognize the track as letter. Also build a CNN recognition model in computer.

## Installation
You can find the demo app in google store and app store. "Ihand" & "ARhand"

## References to hand gesture recognition
* https://github.com/bicho/hand_finger_recognition_android
* http://eaglesky.github.io/2015/12/26/HandGestureRecognition/
* http://www.intorobotics.com/9-opencv-tutorials-hand-gesture-detection-recognition/

