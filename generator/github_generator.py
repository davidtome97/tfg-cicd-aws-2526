from pathlib import Path

from jinja2 import Environment, FileSystemLoader
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4


class GitHubWorkflowGenerator:
    """
    Generador de workflows de GitHub Actions:
    - Pregunto la configuración por consola
    - Genero el YAML desde la plantilla github_ci.yml.j2
    - Genero un PDF con un resumen y un bloque por cada secret
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

        # -------- SONAR --------
        use_sonar = (
                            input("¿Incluir análisis SonarCloud? (s/n) [s]: ").strip().lower() or "s"
                    ) == "s"

        fail_on_sonar = False

        if use_sonar:
            fail_on_sonar = (
                                    input(
                                        "Si falla el Quality Gate de Sonar, ¿quiero que se ROMPA la pipeline? "
                                        "(s/n) [n]: "
                                    ).strip().lower()
                                    or "n"
                            ) == "s"

        # -------- AWS --------
        use_aws = (
                          input("¿Añadir deploy completo a AWS (ECR + EC2)? (s/n) [n]: ")
                          .strip()
                          .lower()
                          or "n"
                  ) == "s"

        deploy_mode = "none"
        aws_secrets: dict[str, str] = {}

        if use_aws:
            print("\n¿Cuándo quiero lanzar el deploy?")
            print(" 1) En cada push a main")
            print(" 2) Solo con tag (release)")
            print(" 3) Solo manual (desde la UI de Actions)")
            opcion = input("Elige 1/2/3 [2]: ").strip() or "2"
            deploy_mode = {"1": "main", "2": "tag", "3": "manual"}.get(opcion, "tag")

            print(
                "\nPara AWS voy a usar NOMBRES de secrets de GitHub.\n"
                "Luego tendré que crear esos secrets con sus valores reales en GitHub."
            )

            usar_defectos = (
                                    input(
                                        "¿Usar nombres de secrets estándar "
                                        "(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, EC2_LLAVE_SSH, etc.)? "
                                        "(s/n) [s]: "
                                    ).strip().lower()
                                    or "s"
                            ) == "s"

            if usar_defectos:
                # Incluyo también EC2_LLAVE_SSH
                aws_secrets = {
                    "access_key": "AWS_ACCESS_KEY_ID",
                    "secret_key": "AWS_SECRET_ACCESS_KEY",
                    "region": "AWS_REGION",
                    "ecr_registry": "AWS_ECR_URL",
                    "ecr_repo": "ECR_REPOSITORY",
                    "ec2_host": "EC2_HOST",
                    "ec2_user": "EC2_USUARIO",
                    "ec2_key": "EC2_LLAVE_SSH",
                }
            else:
                print(
                    "\nIntroduzco los NOMBRES de los secrets (no sus valores). "
                    "Si dejo algo vacío, uso el nombre por defecto entre paréntesis."
                )
                aws_secrets = {
                    "access_key": input(
                        "Nombre secret para AWS access key id [AWS_ACCESS_KEY_ID]: "
                    ).strip()
                                  or "AWS_ACCESS_KEY_ID",
                    "secret_key": input(
                        "Nombre secret para AWS secret access key [AWS_SECRET_ACCESS_KEY]: "
                    ).strip()
                                  or "AWS_SECRET_ACCESS_KEY",
                    "region": input(
                        "Nombre secret para región AWS [AWS_REGION]: "
                    ).strip()
                              or "AWS_REGION",
                    "ecr_registry": input(
                        "Nombre secret para URL del registry ECR [AWS_ECR_URL]: "
                    ).strip()
                                    or "AWS_ECR_URL",
                    "ecr_repo": input(
                        "Nombre secret para nombre del repositorio ECR [ECR_REPOSITORY]: "
                    ).strip()
                                or "ECR_REPOSITORY",
                    "ec2_host": input(
                        "Nombre secret para host/IP de EC2 [EC2_HOST]: "
                    ).strip()
                                or "EC2_HOST",
                    "ec2_user": input(
                        "Nombre secret para usuario de EC2 [EC2_USUARIO]: "
                    ).strip()
                                or "EC2_USUARIO",
                    "ec2_key": input(
                        "Nombre secret para la clave SSH (.pem) de EC2 [EC2_LLAVE_SSH]: "
                    ).strip()
                               or "EC2_LLAVE_SSH",
                }

        # Solo informativo
        use_db = (
                         input("¿Tu proyecto usa base de datos? (s/n) [s]: ").strip().lower() or "s"
                 ) == "s"

        return {
            "project_name": project_name,
            "branches": branches,
            "run_on_pr": run_on_pr,
            "use_node": use_node,
            "use_sonar": use_sonar,
            "fail_on_sonar": fail_on_sonar,
            # Ya no necesito datos de Sonar aquí: se leen de los secrets de GitHub
            "sonar": {},
            "use_aws": use_aws,
            "deploy_mode": deploy_mode,
            "aws_secrets": aws_secrets,
            "use_db": use_db,
            "ci_platform": "github",
        }

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
    # 4. PDF (diseño nuevo, con párrafos envueltos)
    # ==========================================================
    @staticmethod
    def _create_pdf(config: dict, pdf_path: Path) -> None:
        c = canvas.Canvas(str(pdf_path), pagesize=A4)
        width, height = A4
        y = height - 50

        use_sonar = config.get("use_sonar", False)
        use_aws = config.get("use_aws", False)
        aws_secrets_cfg: dict = config.get("aws_secrets") or {}

        # -------- helpers --------
        def new_page():
            nonlocal y
            c.showPage()
            y = height - 50

        def title(text: str):
            nonlocal y
            if y < 80:
                new_page()
            c.setFont("Helvetica-Bold", 16)
            c.drawString(40, y, text)
            y -= 25

        def subtitle(text: str):
            nonlocal y
            if y < 70:
                new_page()
            c.setFont("Helvetica-Bold", 13)
            c.drawString(40, y, text)
            y -= 18

        def line():
            nonlocal y
            if y < 60:
                new_page()
            c.setLineWidth(1)
            c.line(40, y, width - 40, y)
            y -= 12

        def wrap_text(text: str, max_width: int = 90) -> list[str]:
            words = text.split()
            lines: list[str] = []
            current: list[str] = []
            for w in words:
                candidate = (" ".join(current + [w])).strip()
                if len(candidate) <= max_width:
                    current.append(w)
                else:
                    if current:
                        lines.append(" ".join(current))
                    current = [w]
            if current:
                lines.append(" ".join(current))
            return lines

        def paragraph(
                text: str, indent: int = 40, step: int = 14, bold: bool = False
        ):
            nonlocal y
            font = "Helvetica-Bold" if bold else "Helvetica"
            c.setFont(font, 11)
            for line_text in wrap_text(text):
                if y < 60:
                    new_page()
                    c.setFont(font, 11)
                c.drawString(indent, y, line_text)
                y -= step

        def secret_block(nombre: str, ejemplo: str, donde: str):
            nonlocal y
            if y < 120:
                new_page()
            line()
            paragraph(f"Nombre del secret: {nombre}", indent=45, bold=True)
            paragraph(f"Ejemplo de valor: {ejemplo}", indent=60)
            paragraph(f"¿De dónde saco este valor?: {donde}", indent=60)
            y -= 4
            line()
            y -= 4

        # -------- cabecera --------
        title("Resumen del workflow generado para GitHub Actions")

        ramas_txt = ", ".join(config["branches"]) if config["branches"] else "-"
        paragraph(f"Proyecto: {config['project_name']}")
        paragraph(f"Ramas donde corre la CI: {ramas_txt}")
        paragraph(f"¿Ejecuta en pull_request?: {'sí' if config['run_on_pr'] else 'no'}")
        paragraph(f"¿Proyecto con Node?: {'sí' if config['use_node'] else 'no'}")
        paragraph(f"¿Incluye SonarCloud?: {'sí' if use_sonar else 'no'}")
        paragraph(f"¿Incluye deploy a AWS (ECR + EC2)?: {'sí' if use_aws else 'no'}")
        line()

        # ==========================================================
        # SONAR CLOUD (SOLO SI use_sonar ES TRUE)
        # ==========================================================
        if use_sonar:
            subtitle("Secrets necesarios para SonarCloud")

            paragraph(
                "Variables necesarias para que el análisis de SonarCloud funcione "
                "dentro de la pipeline. Uso los mismos nombres que en GitLab para "
                "no duplicar trabajo.",
                indent=40,
            )

            # Como ya no pregunto nada en el formulario, uso ejemplos genéricos:
            sonar_host = "https://sonarcloud.io"
            sonar_project_key = "mi-proyecto_en_sonar"
            sonar_org = "mi-organizacion"

            secret_block(
                "SONAR_HOST_URL",
                sonar_host,
                "Es la URL base de mi servidor SonarCloud. En la mayoría de casos es "
                "https://sonarcloud.io.",
            )
            secret_block(
                "SONAR_PROJECT_KEY",
                sonar_project_key,
                "En SonarCloud voy a mi proyecto, pestaña 'Administration' → 'Update key', "
                "y copio el valor exacto de 'Project key'. Ese valor es el que pego "
                "en este secret.",
            )
            secret_block(
                "SONAR_ORGANIZATION",
                sonar_org,
                "En SonarCloud, arriba a la derecha, puedo ver el identificador de mi "
                "organización (Organization key). Copio ese valor y lo uso en este secret.",
            )
            secret_block(
                "SONAR_TOKEN",
                "Token personal de análisis",
                "En SonarCloud entro con mi usuario, voy a My Account → Security, "
                "creo un token nuevo y copio el valor. Ese valor es el que pego "
                "en este secret.",
            )

        # ==========================================================
        # AWS (SOLO SI use_aws ES TRUE)
        # ==========================================================
        if use_aws:
            subtitle("Secrets necesarios para AWS (ECR + EC2)")

            paragraph(
                "Secrets necesarios para poder hacer login en ECR, subir imágenes Docker "
                "y conectarme por SSH a la instancia EC2 donde despliego.",
                indent=40,
            )

            default_secret_names = {
                "access_key": "AWS_ACCESS_KEY_ID",
                "secret_key": "AWS_SECRET_ACCESS_KEY",
                "region": "AWS_REGION",
                "ecr_registry": "AWS_ECR_URL",
                "ecr_repo": "ECR_REPOSITORY",
                "ec2_host": "EC2_HOST",
                "ec2_user": "EC2_USUARIO",
                "ec2_key": "EC2_LLAVE_SSH",
            }

            examples = {
                "access_key": "AKIAIOSFODNN7EXAMPLE",
                "secret_key": "wJalrXUtnF/.../KEY",
                "region": "eu-west-1",
                "ecr_registry": "490145258703.dkr.ecr.eu-west-1.amazonaws.com",
                "ecr_repo": "tfg-cicd-aws-2526",
                "ec2_host": "ec2-11-22-33-44.eu-west-1.compute.amazonaws.com",
                "ec2_user": "ubuntu",
                "ec2_key": "-----BEGIN PRIVATE KEY----- ... -----END PRIVATE KEY-----",
            }

            where_text = {
                "access_key": (
                    "En AWS Console entro a IAM → Users, selecciono mi usuario y en la pestaña "
                    "'Security credentials' creo una access key. Uso el valor de Access key ID."
                ),
                "secret_key": (
                    "En el mismo sitio donde creo la access key (IAM → Users → Security "
                    "credentials) copio el Secret access key. Ese valor solo se muestra una vez, "
                    "así que lo guardo y lo pego en este secret."
                ),
                "region": (
                    "En la esquina superior derecha de AWS Console selecciono la región en la "
                    "que tengo mis recursos (por ejemplo eu-west-1) y uso ese código."
                ),
                "ecr_registry": (
                    "En AWS Console voy a ECR → Repositories, selecciono mi repositorio y pulso "
                    "el botón 'Copy URI'. De esa URI me quedo con la parte del registry "
                    "(por ejemplo 490145258703.dkr.ecr.eu-west-1.amazonaws.com)."
                ),
                "ecr_repo": (
                    "En AWS Console → ECR → Repositories uso el nombre exacto del repositorio "
                    "Docker donde subo las imágenes (en mi caso tfg-cicd-aws-2526)."
                ),
                "ec2_host": (
                    "En AWS Console voy a EC2 → Instances y copio el valor de 'Public IPv4 DNS' "
                    "o 'Public IPv4 address' de la instancia donde voy a desplegar."
                ),
                "ec2_user": (
                    "Depende de la AMI de la instancia. Para Ubuntu el usuario por defecto es "
                    "'ubuntu' y para Amazon Linux normalmente es 'ec2-user'. Yo utilizo el que "
                    "corresponde a mi máquina."
                ),
                "ec2_key": (
                    "Cuando creo el par de claves de la instancia EC2, AWS me descarga un "
                    "fichero .pem. Guardo ese .pem en mi equipo, le doy permisos con "
                    "'chmod 400 nombre-clave.pem' y lo abro con un editor de texto. Copio "
                    "TODO el contenido de la clave privada, incluyendo las líneas "
                    "'-----BEGIN PRIVATE KEY-----' y '-----END PRIVATE KEY-----', y lo pego "
                    "tal cual dentro de este secret de GitHub."
                ),
            }

            ordered_keys = [
                "access_key",
                "secret_key",
                "region",
                "ecr_registry",
                "ecr_repo",
                "ec2_host",
                "ec2_user",
                "ec2_key",
            ]

            for key in ordered_keys:
                # Usa el nombre del config si existe, si no el por defecto
                secret_name = aws_secrets_cfg.get(key) or default_secret_names[key]
                secret_block(secret_name, examples[key], where_text[key])

        # -------- Cómo crear secrets --------
        subtitle("Cómo creo los secrets en GitHub")

        paragraph("1. Entro en el repositorio de GitHub del proyecto.")
        paragraph("2. Voy a Settings → Secrets and variables → Actions.")
        paragraph("3. Pulso el botón 'New repository secret'.")
        paragraph(
            "4. En 'Name' escribo exactamente el nombre del secret que aparece "
            "en este documento (por ejemplo AWS_ACCESS_KEY_ID, EC2_LLAVE_SSH, "
            "SONAR_HOST_URL, SONAR_PROJECT_KEY, SONAR_ORGANIZATION, SONAR_TOKEN, etc.)."
        )
        paragraph(
            "5. En 'Secret' pego el valor real que he obtenido de AWS o de SonarCloud."
        )
        paragraph(
            "6. Repito estos pasos para cada uno de los secrets hasta tenerlos todos creados."
        )

        c.save()

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