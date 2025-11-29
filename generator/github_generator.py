from pathlib import Path

from jinja2 import Environment, FileSystemLoader

from compartido import ask_common_ci_inputs, create_common_pdf


class GitHubWorkflowGenerator:
    """
    Generador de workflows de GitHub Actions:
    - Pregunta configuración básica de GitHub
    - Pregunta configuración común (Sonar, AWS, BD)
    - Genera el YAML desde la plantilla github_ci.yml.j2
    - Genera un PDF con un resumen y un bloque por cada secret/variable
    """

    def __init__(self) -> None:
        self.root = Path(__file__).resolve().parents[1]
        self.templates_dir = self.root / "generator" / "templates"

    # ==========================================================
    # 1. FORMULARIO
    # ==========================================================
    def ask_inputs(self) -> dict:
        print("=== Generador de Workflows CI/CD (GitHub Actions) ===\n")

        project_name = input(
            "Nombre del proyecto (para el workflow) [mi-proyecto]: "
        ).strip() or "mi-proyecto"

        branches_raw = input(
            "Ramas donde quieres ejecutar CI (coma, ej: main,develop) [main]: "
        ).strip()
        branches = (
            ["main"]
            if not branches_raw
            else [b.strip() for b in branches_raw.split(",") if b.strip()]
        )

        run_on_pr = (
                            input("¿Ejecutar también en pull_request? (s/n) [s]: ").strip().lower()
                            or "s"
                    ) == "s"

        use_node = (
                           input("¿Tu proyecto tiene parte en Node? (s/n) [n]: ").strip().lower()
                           or "n"
                   ) == "s"

        # ---- Parte común (Sonar, AWS, BD) ----
        common = ask_common_ci_inputs(ci_platform="github")

        config = {
            "project_name": project_name,
            "branches": branches,
            "run_on_pr": run_on_pr,
            "use_node": use_node,
            **common,
        }

        return config

    # ==========================================================
    # 2. RENDER YAML
    # ==========================================================
    def render_yaml(self, config: dict) -> str:
        env = Environment(loader=FileSystemLoader(str(self.templates_dir)))
        template = env.get_template("github_ci.yml.j2")
        return template.render(config=config)

    # ==========================================================
    # 3. RUTAS DE SALIDA
    # ==========================================================
    def output_paths(self) -> tuple[Path, Path]:
        yaml_path = self.root / ".github" / "workflows" / "generated-ci.yml"
        pdf_path = self.root / "generator" / "workflow-github.pdf"
        return yaml_path, pdf_path

    @staticmethod
    def save_yaml(content: str, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)

    # ==========================================================
    # 4. PDF (usa la función común)
    # ==========================================================
    @staticmethod
    def _create_pdf(config: dict, pdf_path: Path) -> None:
        create_common_pdf(config, pdf_path)

    # ==========================================================
    # 5. RUN
    # ==========================================================
    def run(self) -> None:
        config = self.ask_inputs()
        yaml_path, pdf_path = self.output_paths()

        yaml_content = self.render_yaml(config)
        self.save_yaml(yaml_content, yaml_path)
        self._create_pdf(config, pdf_path)

        print("\nWorkflow de GitHub generado correctamente.")
        print(f" - YAML: {yaml_path}")
        print(f" - PDF:  {pdf_path}")