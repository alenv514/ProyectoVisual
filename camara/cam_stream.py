import sys
import time
import cv2

def main():
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        sys.exit(1)
    
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    
    encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 65]
    
    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                break
                
            h, w = frame.shape[:2]
            # Retícula de encuadre facial
            cv2.rectangle(frame, (w // 4, h // 6), (3 * w // 4, 5 * h // 6), (46, 204, 113), 2)
            cv2.putText(frame, "CAMARA EN VIVO - VERIFICACION VISUAL", (20, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.55, (46, 204, 113), 2)
            cv2.putText(frame, "ENFOQUE EL ROSTRO AL CENTRO", (w // 4, 5 * h // 6 + 25),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.45, (255, 255, 255), 1)

            ok, buf = cv2.imencode('.jpg', frame, encode_param)
            if not ok:
                continue
                
            data = buf.tobytes()
            sys.stdout.buffer.write(len(data).to_bytes(4, byteorder='big'))
            sys.stdout.buffer.write(data)
            sys.stdout.buffer.flush()
            time.sleep(0.033)
    except (BrokenPipeError, KeyboardInterrupt):
        pass
    finally:
        cap.release()

if __name__ == '__main__':
    main()
