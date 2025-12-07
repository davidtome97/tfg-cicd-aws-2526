from pathlib import Path

from jinja2 import Environment, FileSystemLoader

from compartido import ask_common_ci_inputs, create_common_pdf


class GitHubWorkflowGenerator:
    """
    Aquí defino el generador para workflows de GitHub Actions.
    - Pido la configuración necesaria al usuario
    - Junto eso con la configuración común (Sonar, AWS, BD)
    - Renderizo el YAML usando la plantilla
    - Y creo un PDF resumen con todos los datos
    """

    def __init__(self) -> None:
        # Obtengo la ruta raíz del proyecto y dejo guardada la carpeta de plantillas.
        self.root = Path(__file__).resolve().parents[1]
        self.templates_dir = self.root / "generator" / "templates"

    # PARTE DE FORMULARIO
    def ask_inputs(self) -> dict:
        print("=== Generador de Workflows CI/CD (GitHub Actions) ===\n")

        # Nombre del proyecto para ponerlo en el workflow.
        project_name = input(
            "Nombre del proyecto (para el workflow) [mi-proyecto]: "
        ).strip() or "mi-proyecto"

        # Ramas donde quiero ejecutar la CI.
        branches_raw = input(
            "Ramas donde quieres ejecutar CI (coma, ej: main,develop) [main]: "
        ).strip()
        branches = (
            ["main"]
            if not branches_raw
            else [b.strip() for b in branches_raw.split(",") if b.strip()]
        )

        # Si también quiero que se ejecute en PR.
        run_on_pr = (
                            input("¿Ejecutar también en pull_request? (s/n) [s]: ").strip().lower()
                            or "s"
                    ) == "s"

        # Si el proyecto tiene parte en Node para generar el job extra.
        use_node = (
                           input("¿Tu proyecto tiene parte en Node? (s/n) [n]: ").strip().lower()
                           or "n"
                   ) == "s"

        # Parte común que reutilizo para sonar, aws y base de datos.
        common = ask_common_ci_inputs(ci_platform="github")

        # aqui voy juntando la configuración final en un diccionario.
        config = {
            "project_name": project_name,
            "branches": branches,
            "run_on_pr": run_on_pr,
            "use_node": use_node,
            **common,  # Aquí añado todo lo que viene del formulario común.
        }

        return config

    # PARTE DEL RENDER YAML

    def render_yaml(self, config: dict) -> str:
        # Cargo el entorno Jinja apuntando a la carpeta de plantillas.
        env = Environment(loader=FileSystemLoader(str(self.templates_dir)))
        # Cojo la plantilla de GitHub CI.
        template = env.get_template("github_ci.yml.j2")
        # Y la renderizo usando los valores introducidos.
        return template.render(config=config)

    # PARTE DE RUTAS DE SALIDA

    def output_paths(self) -> tuple[Path, Path]:
        # Ruta donde dejo el YAML generado dentro de .github/workflows.
        yaml_path = self.root / ".github" / "workflows" / "generated-ci.yml"
        # Ruta donde dejo el PDF de resumen.
        pdf_path = self.root / "generator" / "workflow-github.pdf"
        return yaml_path, pdf_path

    @staticmethod
    def save_yaml(content: str, path: Path) -> None:
        # Me aseguro de que existe la carpeta antes de guardar.
        path.parent.mkdir(parents=True, exist_ok=True)
        # Escribo el contenido del YAML generado.
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)

    # PARTE DEL PDF
    @staticmethod
    def _create_pdf(config: dict, pdf_path: Path) -> None:
        # Uso el creador común de PDFs para generar el informe.
        create_common_pdf(config, pdf_path)

    # PARTE DEL RUN GENERAL

    def run(self) -> None:
        # Primero pido todas las entradas.
        config = self.ask_inputs()

        # Obtengo las rutas de salida del YAML y el PDF.
        yaml_path, pdf_path = self.output_paths()

        # Renderizo el YAML y lo guardo en su archivo.
        yaml_content = self.render_yaml(config)
        self.save_yaml(yaml_content, yaml_path)

        # Genero también el PDF resumen.
        self._create_pdf(config, pdf_path)

        # Mensaje final de éxito.
        print("\nWorkflow de GitHub generado correctamente.")
        print(f" - YAML: {yaml_path}")
        print(f" - PDF:  {pdf_path}")