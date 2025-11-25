from pathlib import Path

from jinja2 import Environment, FileSystemLoader
from reportlab.pdfgen import canvas


class GitHubWorkflowGenerator:
    """
    Generador de workflows de GitHub Actions:
    - Pide datos por consola
    - Renderiza templates/github_ci.yml.j2
    - Crea workflow-github.pdf con instrucciones
    """

    def __init__(self) -> None:
        self.root = Path(__file__).resolve().parents[1]
        self.templates_dir = self.root / "generator" / "templates"

    # -------------------------- INPUTS -------------------------- #
    def ask_inputs(self) -> dict:
        print("=== Generador de Workflows CI/CD (GitHub Actions) ===\n")

        project_name = input(
            "Nombre del proyecto (para el nombre del workflow) [mi-proyecto]: "
        ).strip() or "mi-proyecto"

        branches_raw = input(
            "Ramas donde quieres que se ejecute CI "
            "(separadas por coma, ej: main,develop) [main]: "
        ).strip()

        if not branches_raw:
            branches = ["main"]
        else:
            branches = [b.strip() for b in branches_raw.split(",") if b.strip()]

        run_on_pr = (
                            input("¿Quieres que se ejecute también en pull_request? (s/n) [s]: ")
                            .strip()
                            .lower()
                            or "s"
                    ) == "s"

        # --- Sonar ---
        use_sonar = (
                            input("¿Quieres incluir análisis de Sonar? (s/n) [s]: ")
                            .strip()
                            .lower()
                            or "s"
                    ) == "s"

        fail_on_sonar = False
        sonar_host = ""
        sonar_project_key = ""
        sonar_org = ""

        if use_sonar:
            fail_on_sonar = (
                                    input(
                                        "Si Sonar falla (quality gate), ¿debe ROMPER la pipeline? (s/n) [n]: "
                                    )
                                    .strip()
                                    .lower()
                                    or "n"
                            ) == "s"

            sonar_host = input(
                "URL de Sonar (ej: https://sonarcloud.io) [vacío]: "
            ).strip()
            sonar_project_key = input("sonar.projectKey [vacío]: ").strip()
            sonar_org = input("sonar.organization [vacío]: ").strip()

        # --- AWS / Deploy ---
        use_aws = (
                          input("¿Quieres añadir un job de deploy a AWS (build+push ECR)? (s/n) [n]: ")
                          .strip()
                          .lower()
                          or "n"
                  ) == "s"

        deploy_mode = "none"  # none | main | tag | manual
        aws_secrets = {}

        if use_aws:
            print("\n¿Cuándo quieres que se dispare el deploy?")
            print("  1) En cada push a main")
            print("  2) Solo cuando haya un tag (release)")
            print("  3) Solo manual (desde la UI de Actions)")
            opcion = (input("Elige 1/2/3 [2]: ").strip() or "2").strip()

            if opcion == "1":
                deploy_mode = "main"
            elif opcion == "2":
                deploy_mode = "tag"
            else:
                deploy_mode = "manual"

            print(
                "\nPara AWS vamos a usar NOMBRES de secrets de GitHub.\n"
                "Tú luego tendrás que crear esos secrets con sus valores reales en GitHub."
            )

            usar_defectos = (
                                    input(
                                        "¿Usar nombres de secrets AWS por defecto "
                                        "(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, etc.)? (s/n) [s]: "
                                    )
                                    .strip()
                                    .lower()
                                    or "s"
                            ) != "n"

            if usar_defectos:
                aws_secrets = {
                    "access_key": "AWS_ACCESS_KEY_ID",
                    "secret_key": "AWS_SECRET_ACCESS_KEY",
                    "region": "AWS_REGION",
                    "ecr_registry": "AWS_ECR_REGISTRY",
                    "ecr_repo": "AWS_ECR_REPOSITORY",
                    "ec2_host": "AWS_EC2_HOST",
                    "ec2_user": "AWS_EC2_USER",
                }
            else:
                print(
                    "\nIntroduce los NOMBRES de los secrets que quieres usar (no valores):"
                )
                aws_secrets = {
                    "access_key": input(
                        "Nombre del secret para AWS access key id: "
                    ).strip()
                                  or "AWS_ACCESS_KEY_ID",
                    "secret_key": input(
                        "Nombre del secret para AWS secret access key: "
                    ).strip()
                                  or "AWS_SECRET_ACCESS_KEY",
                    "region": input("Nombre del secret para región AWS: ").strip()
                              or "AWS_REGION",
                    "ecr_registry": input(
                        "Nombre del secret para URL del registry ECR: "
                    ).strip()
                                    or "AWS_ECR_REGISTRY",
                    "ecr_repo": input(
                        "Nombre del secret para nombre del repositorio ECR: "
                    ).strip()
                                or "AWS_ECR_REPOSITORY",
                    "ec2_host": input(
                        "Nombre del secret para host/IP de EC2: "
                    ).strip()
                                or "AWS_EC2_HOST",
                    "ec2_user": input(
                        "Nombre del secret para usuario de EC2: "
                    ).strip()
                                or "AWS_EC2_USER",
                }

        use_db = (
                         input("¿Tu proyecto usa base de datos? (s/n) [s]: ").strip().lower() or "s"
                 ) == "s"

        return {
            "project_name": project_name,
            "branches": branches,
            "run_on_pr": run_on_pr,
            "use_sonar": use_sonar,
            "fail_on_sonar": fail_on_sonar,
            "sonar": {
                "host": sonar_host,
                "project_key": sonar_project_key,
                "organization": sonar_org,
            },
            "use_aws": use_aws,
            "deploy_mode": deploy_mode,  # none | main | tag | manual
            "aws_secrets": aws_secrets,
            "use_db": use_db,
            "ci_platform": "github",
        }

    # -------------------------- RENDER -------------------------- #
    def render_yaml(self, config: dict) -> str:
        env = Environment(loader=FileSystemLoader(str(self.templates_dir)))
        template = env.get_template("github_ci.yml.j2")
        return template.render(config=config)

    def output_paths(self) -> tuple[Path, Path]:
        yaml_path = self.root / ".github" / "workflows" / "generated-ci.yml"
        pdf_path = self.root / "generator" / "workflow-github.pdf"
        return yaml_path, pdf_path

    @staticmethod
    def save_yaml(content: str, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)

    # -------------------------- PDF -------------------------- #
    @staticmethod
    def _create_pdf(config: dict, pdf_path: Path) -> None:
        c = canvas.Canvas(str(pdf_path))
        y = 800

        def write(text: str, indent: int = 40, step: int = 15):
            nonlocal y
            if y < 60:
                c.showPage()
                y = 800
            c.drawString(indent, y, text)
            y -= step

        # Cabecera
        write("Resumen workflow GitHub generado", 40, 30)
        write(f"Proyecto: {config['project_name']}")
        write("Plataforma CI: GitHub Actions")
        ramas_txt = ", ".join(config["branches"]) if config["branches"] else "-"
        write(f"Ramas CI: {ramas_txt}")
        write(f"PR activado: {'sí' if config['run_on_pr'] else 'no'}", 40, 20)

        # Sonar
        write(f"Sonar: {'sí' if config['use_sonar'] else 'no'}", 40, 20)
        if config["use_sonar"]:
            write(
                f"Quality gate estricto: {'sí' if config['fail_on_sonar'] else 'no'}",
                40,
                20,
            )
            if config["sonar"].get("host"):
                write(f"Host Sonar: {config['sonar']['host']}")
            if config["sonar"].get("project_key"):
                write(f"sonar.projectKey: {config['sonar']['project_key']}")
            if config["sonar"].get("organization"):
                write(f"sonar.organization: {config['sonar']['organization']}")
            y -= 10

        # AWS
        write(f"AWS (deploy): {'sí' if config['use_aws'] else 'no'}", 40, 20)
        if config["use_aws"]:
            modos = {"main": "push a main", "tag": "tag (release)", "manual": "manual"}
            modo_txt = modos.get(config["deploy_mode"], config["deploy_mode"])
            write(f"Modo deploy: {modo_txt}", 40, 25)

            write("Nombres de secrets AWS (GitHub Secrets):", 40, 20)
            etiquetas = {
                "access_key": "Access Key ID",
                "secret_key": "Secret Access Key",
                "region": "Región AWS",
                "ecr_registry": "Registry ECR",
                "ecr_repo": "Repositorio ECR",
                "ec2_host": "Host/IP EC2",
                "ec2_user": "Usuario EC2",
            }
            for key, secret_name in config["aws_secrets"].items():
                legible = etiquetas.get(key, key)
                write(f"- {legible}: {secret_name}", 60, 15)

        # BD
        y -= 10
        write(
            f"Base de datos: {'sí' if config['use_db'] else 'no'} (solo informativo)",
            40,
            20,
        )
        write(
            "Recuerda crear los secrets en GitHub → Settings → Secrets and variables → Actions.",
            40,
            20,
        )

        # Instrucciones detalladas
        y -= 20
        write("¿De dónde saco estos valores?", 40, 20)

        if config["use_sonar"]:
            write("Token de SonarCloud (SONAR_TOKEN):", 40, 18)
            write("1. Entra en SonarCloud con tu usuario.", 60)
            write("2. Arriba a la derecha → My Account → Security.", 60)
            write("3. Crea un token nuevo (scope: analysis).", 60)
            write("4. Copia el token y guárdalo (solo se muestra una vez).", 60)
            write("5. Crea el secret SONAR_TOKEN en GitHub con ese valor.", 60)
            y -= 10

        if config["use_aws"]:
            write("Credenciales de AWS (Access key / Secret key):", 40, 18)
            write("1. Entra en AWS Console → IAM → Users.", 60)
            write("2. Selecciona un usuario con permisos para ECR/EC2.", 60)
            write("3. Pestaña 'Security credentials' → Create access key.", 60)
            write("4. Copia 'Access key ID' y 'Secret access key'.", 60)
            write(
                "5. Crea los secrets AWS_ACCESS_KEY_ID y AWS_SECRET_ACCESS_KEY.", 60
            )
            y -= 10

            write("Otros valores AWS necesarios:", 40, 18)
            write("- AWS_REGION: ej. eu-west-1", 60)
            write("- AWS_ECR_REGISTRY: copiar desde AWS ECR (Copy URI).", 60)
            write("- AWS_ECR_REPOSITORY: nombre del repo de ECR.", 60)
            write("- AWS_EC2_HOST: IP pública / DNS de la instancia EC2.", 60)
            write("- AWS_EC2_USER: ej. ubuntu / ec2-user.", 60)
            y -= 10

        write("Cómo crear secrets en GitHub:", 40, 18)
        write("1. Abre tu repositorio en GitHub.", 60)
        write("2. Ve a Settings → Secrets and variables → Actions.", 60)
        write("3. Pulsa 'New repository secret'.", 60)
        write("4. Escribe el NOMBRE EXACTO del secret que aparece en este PDF.", 60)
        write("5. Pega el valor correspondiente.", 60)
        write("6. Repite el proceso para cada secret necesario.", 60)

        c.showPage()
        c.save()

    # -------------------------- RUN -------------------------- #
    def run(self) -> None:
        config = self.ask_inputs()
        yaml_content = self.render_yaml(config)
        yaml_path, pdf_path = self.output_paths()

        self.save_yaml(yaml_content, yaml_path)
        self._create_pdf(config, pdf_path)

        print("\n✅ Workflow de GitHub generado correctamente:")
        print(f" - YAML: {yaml_path}")
        print(f" - PDF:  {pdf_path}")