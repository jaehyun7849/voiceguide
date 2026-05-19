# YOLO26n TFLite 모델 적용 및 검증

실제 장애물 감지 모델은 아래 경로에 둡니다.

```text
app/src/main/assets/obstacle_detector.tflite
```

## 현재 상태

현재 앱 asset의 `obstacle_detector.tflite`는 `voiceguide-export` 환경에서 공식 Ultralytics `yolo26n.pt`를 다운로드한 뒤 `--imgsz 320`으로 export한 YOLO26n TFLite 모델입니다.

## export 환경

export 기준 conda 환경은 `voiceguide-export`입니다.

```powershell
conda activate voiceguide-export
python --version
python -m pip show ultralytics
```

필요 패키지가 빠져 있으면 아래처럼 설치합니다.

```powershell
python -m pip install tensorflow==2.19.1 tf_keras==2.19.0 keras==3.14.1 numpy==2.1.3 tensorboard==2.19.0 typing_extensions==4.15.0 protobuf==5.29.6 grpcio==1.80.0 onnx==1.21.0 ml_dtypes==0.5.4 sng4onnx onnx_graphsurgeon onnx2tf onnxslim onnxruntime
python -m pip check
```

`tflite_support` 설치가 C++ 빌드 도구 문제로 실패하면, 현재 export 스크립트의 `--skip-metadata` 옵션을 사용합니다. 모델 추론 자체에는 TFLite metadata가 필수는 아닙니다.

## YOLO26n export

YOLO26n 원본이 생기면 아래 명령으로 앱 asset을 교체합니다.

```powershell
python tools\export_yolo_to_tflite.py path\to\yolo26n.pt --imgsz 320 --skip-metadata --output app\src\main\assets\obstacle_detector.tflite
```

## 실제 detector 빌드

디버그 빌드에서 실제 TFLite detector를 사용하려면 Gradle property를 켭니다.

```powershell
& 'C:\Users\kjh\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat' :app:assembleDebug '-Pvoiceguide.useTfliteDetector=true' --console=plain
```

APK 설치와 실행:

```powershell
& 'C:\Users\kjh\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s R5CRA1DK9FM install -r app\build\outputs\apk\debug\app-debug.apk
& 'C:\Users\kjh\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s R5CRA1DK9FM shell am start -n com.voiceguide/.MainActivity
```

## MVP 검증 기준

앱 화면의 성능 오버레이에서 아래 값을 확인합니다.

```text
analysis
YUV->NV21
input
inference
postprocess
total
detections
```

positive case 검증은 사람, 의자, 테이블처럼 모델이 지원하는 대상을 카메라에 비춘 뒤 아래 조건을 확인합니다.

- 초록색 박스가 대상 위치에 맞게 그려지는지
- 라벨과 confidence가 표시되는지
- `detections` 값이 1 이상으로 올라가는지
- 안내 음성이 너무 자주 또는 너무 늦게 나오지 않는지
- 2 FPS에서 발열이 크면 1 FPS 또는 1.33 FPS 설정으로 비교할 수 있는지

YOLO26n 원본 모델이 없는 상태에서는 “실제 detector 파이프라인 검증”까지만 완료로 보고, “YOLO26n 적용 완료”라고 판단하지 않습니다.
