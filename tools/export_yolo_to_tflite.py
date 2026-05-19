import argparse
import os
import shutil
import sys
from pathlib import Path

from ultralytics import YOLO


def main() -> None:
    parser = argparse.ArgumentParser(description="Export a YOLO .pt model to the app TFLite asset.")
    parser.add_argument("pt_model", type=Path, help="Path to the source .pt model, for example yolo26n.pt.")
    parser.add_argument("--imgsz", type=int, default=640, help="Square export image size.")
    parser.add_argument("--int8", action="store_true", help="Export an INT8 model if calibration data is configured.")
    parser.add_argument(
        "--skip-metadata",
        action="store_true",
        help="Skip TFLite metadata injection when tflite_support is unavailable.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("app/src/main/assets/obstacle_detector.tflite"),
        help="Destination asset path.",
    )
    parser.add_argument(
        "--expected-conda-env",
        default="voiceguide-export",
        help="Warn when export is not running inside this conda environment.",
    )
    args = parser.parse_args()

    active_env = os.environ.get("CONDA_DEFAULT_ENV") or Path(sys.prefix).name
    if args.expected_conda_env and active_env != args.expected_conda_env:
        print(
            f"Warning: expected conda env '{args.expected_conda_env}', "
            f"but active env is '{active_env or 'unknown'}'.",
            file=sys.stderr,
        )
    if not args.pt_model.exists() and not is_ultralytics_model_name(args.pt_model):
        raise FileNotFoundError(
            f"Source model not found: {args.pt_model}. "
            "Provide the real YOLO26n .pt file before claiming YOLO26n is applied."
        )

    if args.skip_metadata:
        patch_ultralytics_tflite_metadata()

    exported = Path(YOLO(str(args.pt_model)).export(format="tflite", imgsz=args.imgsz, int8=args.int8))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(exported, args.output)
    print(f"Copied {exported} -> {args.output}")


def is_ultralytics_model_name(path: Path) -> bool:
    return path.parent == Path(".") and path.suffix == ".pt"


def patch_ultralytics_tflite_metadata() -> None:
    import ultralytics.engine.exporter as exporter

    original_check_requirements = exporter.check_requirements

    def check_requirements_without_tflite_support(requirements=(), *args, **kwargs):
        if isinstance(requirements, str):
            filtered = None if requirements.startswith("tflite_support") else requirements
        else:
            filtered = tuple(
                requirement
                for requirement in requirements
                if not str(requirement).startswith("tflite_support")
            )
        if not filtered:
            return True
        return original_check_requirements(filtered, *args, **kwargs)

    exporter.check_requirements = check_requirements_without_tflite_support
    exporter.Exporter._add_tflite_metadata = lambda self, file: None


if __name__ == "__main__":
    main()
