from pathlib import Path

from jinja2 import Environment, FileSystemLoader

from compartido import ask_common_ci_inputs, create_common_pdf


class GitLabWorkflowGenerator:
    """
    Generador del fichero .gitlab-ci.yml.
    Aquí hago el generador de GitLab:
    - Pido los datos básicos del pipeline
    - Pido también la parte común (Sonar, AWS y BD)
    - Renderizo la plantilla específica de GitLab
    - Y genero un PDF resumen con la configuración
    """

    def __init__(self) -> None:
        # Guardo la ruta raíz del proyecto y la ruta a las plantillas Jinja.
        self.root = Path(__file__).resolve().parents[1]
        self.templates_dir = self.root / "generator" / "templates"

    # PARTE DEL INPUTS
    def ask_inputs(self) -> dict:
        print("=== Generador GitLab CI/CD ===\n")

        # Nombre del proyecto que quiero que aparezca en el pipeline.
        project_name = (
                input("Nombre del proyecto (para el pipeline) [mi-proyecto]: ").strip()
                or "mi-proyecto"
        )

        # Ramas donde quiero que se ejecute la pipeline.
        branches_raw = input(
            "Ramas donde quieres que se ejecute CI (ej: main,develop) [main]: "
        ).strip()
        branches = (
            ["main"]
            if not branches_raw
            else [b.strip() for b in branches_raw.split(",") if b.strip()]
        )

        # La parte común para Sonar, AWS y BD la reutilizo del módulo compartido.
        common = ask_common_ci_inputs(ci_platform="gitlab")

        # Aquí monto la configuración completa.
        config = {
            "project_name": project_name,
            "branches": branches,
            # Este generador no usa PRs ni Node en GitLab, pero dejo los campos por compatibilidad.
            "run_on_pr": False,
            "use_node": False,
            **common,
        }
        return config

    # PARTE DEL YAML
    def render_yaml(self, config: dict) -> str:
        # Cargo el entorno con la carpeta de plantillas.
        env = Environment(loader=FileSystemLoader(str(self.templates_dir)))
        # Cojo la plantilla del pipeline de GitLab.
        template = env.get_template("gitlab_ci.yml.j2")
        # La renderizo con los valores recogidos del formulario.
        return template.render(config=config)

    # PARTE DE LAS SALIDAS
    def output_paths(self) -> tuple[Path, Path]:
        # El YAML generado se guarda directamente en la raíz como .gitlab-ci.yml.
        yaml_path = self.root / ".gitlab-ci.yml"
        # El PDF lo dejo en la carpeta generator igual que en GitHub.
        pdf_path = self.root / "generator" / "workflow-gitlab.pdf"
        return yaml_path, pdf_path

    @staticmethod
    def save_yaml(content: str, path: Path) -> None:
        # Creo la carpeta si no existe.
        path.parent.mkdir(parents=True, exist_ok=True)
        # Y escribo el YAML generado.
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)

    # PARTE DEL PDF
    @staticmethod
    def _create_pdf(config: dict, pdf_path: Path) -> None:
        # Uso la función común para generar el PDF de resumen.
        create_common_pdf(config, pdf_path)

    # PARTE DEL RUN
    def run(self) -> None:
        # Primero recojo todos los inputs.
        config = self.ask_inputs()
        yaml_path, pdf_path = self.output_paths()

        # Renderizo el YAML y lo escribo en su ruta final.
        yaml_content = self.render_yaml(config)
        self.save_yaml(yaml_content, yaml_path)

        # Luego genero el PDF con la configuración.
        self._create_pdf(config, pdf_path)

        # Mensaje final confirmando la generación.
        print("\nPipeline de GitLab generado correctamente.")
        print(f" - YAML: {yaml_path}")
        print(f" - PDF:  {pdf_path}")