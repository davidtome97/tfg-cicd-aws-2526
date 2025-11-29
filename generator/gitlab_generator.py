from pathlib import Path

from jinja2 import Environment, FileSystemLoader

from compartido import ask_common_ci_inputs, create_common_pdf


class GitLabWorkflowGenerator:
    """
    Generador de .gitlab-ci.yml:
    - Pregunta configuración básica de GitLab (nombre, ramas)
    - Pregunta configuración común (Sonar, AWS, BD)
    - Renderiza templates/gitlab_ci.yml.j2
    - Crea generator/workflow-gitlab.pdf
    """

    def __init__(self) -> None:
        self.root = Path(__file__).resolve().parents[1]
        self.templates_dir = self.root / "generator" / "templates"

    # -------------------------- INPUTS -------------------------- #
    def ask_inputs(self) -> dict:
        print("=== Generador GitLab CI/CD ===\n")

        project_name = (
                input("Nombre del proyecto (para el pipeline) [mi-proyecto]: ").strip()
                or "mi-proyecto"
        )

        branches_raw = input(
            "Ramas donde quieres que se ejecute CI (ej: main,develop) [main]: "
        ).strip()
        branches = (
            ["main"]
            if not branches_raw
            else [b.strip() for b in branches_raw.split(",") if b.strip()]
        )

        # Parte común (Sonar, AWS, BD), misma que GitHub
        common = ask_common_ci_inputs(ci_platform="gitlab")

        config = {
            "project_name": project_name,
            "branches": branches,
            "run_on_pr": False,   # GitLab no usa este campo, pero lo dejamos por compatibilidad
            "use_node": False,    # En este generador no estamos manejando Node en GitLab
            **common,
        }
        return config

    # -------------------------- YAML -------------------------- #
    def render_yaml(self, config: dict) -> str:
        env = Environment(loader=FileSystemLoader(str(self.templates_dir)))
        template = env.get_template("gitlab_ci.yml.j2")
        return template.render(config=config)

    # ---------------------- SALIDAS --------------------------- #
    def output_paths(self) -> tuple[Path, Path]:
        yaml_path = self.root / ".gitlab-ci.yml"
        pdf_path = self.root / "generator" / "workflow-gitlab.pdf"
        return yaml_path, pdf_path

    @staticmethod
    def save_yaml(content: str, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)

    # --------------------------- PDF --------------------------- #
    @staticmethod
    def _create_pdf(config: dict, pdf_path: Path) -> None:
        create_common_pdf(config, pdf_path)

    # --------------------------- RUN --------------------------- #
    def run(self) -> None:
        config = self.ask_inputs()
        yaml_path, pdf_path = self.output_paths()

        yaml_content = self.render_yaml(config)
        self.save_yaml(yaml_content, yaml_path)
        self._create_pdf(config, pdf_path)

        print("\nPipeline de GitLab generado correctamente.")
        print(f" - YAML: {yaml_path}")
        print(f" - PDF:  {pdf_path}")