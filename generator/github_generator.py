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
        # Localizo la raíz del proyecto para poder resolver rutas relativas
        self.root = Path(__file__).resolve().parents[1]
        # Carpeta donde tengo las plantillas Jinja2
        self.templates_dir = self.root / "generator" / "templates"

    # -------------------------- INPUTS -------------------------- #
    def ask_inputs(self) -> dict:
        print("=== Generador de Workflows CI/CD (GitHub Actions) ===\n")

        # Nombre que quiero que aparezca en el workflow de GitHub
        project_name = input(
            "Nombre del proyecto (para el nombre del workflow) [mi-proyecto]: "
        ).strip() or "mi-proyecto"

        # Ramas donde quiero que se dispare la CI
        branches_raw = input(
            "Ramas donde quieres que se ejecute CI "
            "(separadas por coma, ej: main,develop) [main]: "
        ).strip()

        if not branches_raw:
            branches = ["main"]
        else:
            branches = [b.strip() for b in branches_raw.split(",") if b.strip()]

        # Decido si también quiero que la CI se ejecute en pull_request
        run_on_pr = (
                            input("¿Quieres que se ejecute también en pull_request? (s/n) [s]: ")
                            .strip()
                            .lower()
                            or "s"
                    ) == "s"

        # Pregunto si el proyecto también tiene parte en Node (por ejemplo un frontend)
        use_node = (
                           input("¿Tu proyecto también tiene parte en Node (frontend, etc.)? (s/n) [n]: ")
                           .strip()
                           .lower()
                           or "n"
                   ) == "s"

        # --- Sonar ---
        # Aquí decido si quiero integrar análisis de calidad con SonarCloud
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
            # Si fallo el quality gate, decido si quiero que se rompa la pipeline o no
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
        # Aquí decido si quiero tener despliegue completo a AWS (ECR + EC2)
        use_aws = (
                          input("¿Quieres añadir deploy completo a AWS (ECR + EC2)? (s/n) [n]: ")
                          .strip()
                          .lower()
                          or "n"
                  ) == "s"

        deploy_mode = "none"  # none | main | tag | manual
        aws_secrets: dict[str, str] = {}

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
                "\nPara AWS voy a usar NOMBRES de secrets de GitHub.\n"
                "Luego tendré que crear esos secrets con sus valores reales en GitHub."
            )

            # Puedo usar los nombres de secrets por defecto o escribir los míos
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
                # En este caso uso los nombres estándar que ya estoy utilizando en el workflow
                aws_secrets = {
                    "access_key": "AWS_ACCESS_KEY_ID",
                    "secret_key": "AWS_SECRET_ACCESS_KEY",
                    "region": "AWS_REGION",
                    "ecr_registry": "AWS_ECR_URL",
                    "ecr_repo": "ECR_REPOSITORY",
                    "ec2_host": "EC2_HOST",
                    "ec2_user": "EC2_USUARIO",
                }
            else:
                # Aquí puedo personalizar los nombres de todos los secrets
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
                                    or "AWS_ECR_URL",
                    "ecr_repo": input(
                        "Nombre del secret para nombre del repositorio ECR: "
                    ).strip()
                                or "ECR_REPOSITORY",
                    "ec2_host": input(
                        "Nombre del secret para host/IP de EC2: "
                    ).strip()
                                or "EC2_HOST",
                    "ec2_user": input(
                        "Nombre del secret para usuario de EC2: "
                    ).strip()
                                or "EC2_USUARIO",
                }

        # Esta pregunta solo la uso a nivel informativo en el PDF
        use_db = (
                         input("¿Tu proyecto usa base de datos? (s/n) [s]: ").strip().lower() or "s"
                 ) == "s"

        # Devuelvo toda la configuración para que la plantilla Jinja pueda generar el YAML
        return {
            "project_name": project_name,
            "branches": branches,
            "run_on_pr": run_on_pr,
            "use_node": use_node,
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
        # Cargo el entorno de plantillas Jinja2 y renderizo el YAML
        env = Environment(loader=FileSystemLoader(str(self.templates_dir)))
        template = env.get_template("github_ci.yml.j2")
        return template.render(config=config)

    def output_paths(self) -> tuple[Path, Path]:
        # Aquí defino dónde quiero guardar el YAML y el PDF que genero
        yaml_path = self.root / ".github" / "workflows" / "generated-ci.yml"
        pdf_path = self.root / "generator" / "workflow-github.pdf"
        return yaml_path, pdf_path

    @staticmethod
    def save_yaml(content: str, path: Path) -> None:
        # Me aseguro de que la carpeta existe y escribo el YAML en disco
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)

    # -------------------------- PDF -------------------------- #
    @staticmethod
    def _create_pdf(config: dict, pdf_path: Path) -> None:
        """
        Creo un PDF explicativo con:
        - Resumen de configuración
        - Requisitos del proyecto (mvnw / pom.xml)
        - Secrets necesarios en GitHub (Sonar / AWS)
        - Dónde conseguir cada valor (SonarCloud, IAM, ECR, EC2…)
        - Cómo crear los secrets en GitHub
        """
        c = canvas.Canvas(str(pdf_path))
        y = 800

        def write(text: str, indent: int = 40, step: int = 15):
            # Pequeña función para escribir líneas con salto de página automático
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
        write(
            f"Proyecto con Node: {'sí' if config.get('use_node') else 'no'}",
            40,
            20,
        )

        # Requisitos del proyecto
        y -= 10
        write("Requisitos del proyecto para este workflow:", 40, 20)
        write("- Proyecto Java con Maven Wrapper (./mvnw).", 60)
        write("- Fichero pom.xml (el workflow llama a -f pom.xml).", 60)
        write(
            "- Si el código está en otra ruta, tengo que adaptar las rutas -f pom.xml en el YAML.",
            60,
        )
        write(
            "- Si no tengo mvnw, puedo cambiar ./mvnw por mvn en los pasos de Maven.",
            60,
        )
        if config.get("use_node"):
            write(
                "- Si uso Node, necesito package.json con scripts de test/lint si quiero aprovecharlos.",
                60,
            )

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

            # Secrets Sonar
            write("Secrets necesarios para SonarCloud:", 40, 18)
            write("- SONAR_HOST_URL  (ej: https://sonarcloud.io)", 60)
            write(
                "- SONAR_TOKEN     (token personal de SonarCloud para análisis)",
                60,
            )
            y -= 10

        # AWS
        write(f"AWS (deploy): {'sí' if config['use_aws'] else 'no'}", 40, 20)
        if config["use_aws"]:
            modos = {"main": "push a main", "tag": "tag (release)", "manual": "manual"}
            modo_txt = modos.get(config["deploy_mode"], config["deploy_mode"])
            write(f"Modo deploy: {modo_txt}", 40, 25)

            write("Secrets AWS utilizados en el workflow:", 40, 20)
            etiquetas_legibles = {
                "access_key": "Access Key ID",
                "secret_key": "Secret Access Key",
                "region": "Región AWS",
                "ecr_registry": "Registry ECR",
                "ecr_repo": "Repositorio ECR",
                "ec2_host": "Host/IP EC2",
                "ec2_user": "Usuario EC2",
            }
            ejemplos_valor = {
                "access_key": "AKIAIOSFODNN7EXAMPLE",
                "secret_key": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY",
                "region": "eu-west-1",
                "ecr_registry": "123456789012.dkr.ecr.eu-west-1.amazonaws.com",
                "ecr_repo": "tfg-cicd-aws-2526",
                "ec2_host": "ec2-11-22-33-44.eu-west-1.compute.amazonaws.com",
                "ec2_user": "ubuntu",
            }

            for key, secret_name in config["aws_secrets"].items():
                legible = etiquetas_legibles.get(key, key)
                ejemplo = ejemplos_valor.get(key, "valor-ejemplo")
                write(
                    f"- {legible}: {secret_name}  (ejemplo de valor: {ejemplo})",
                    60,
                    15,
                )

        # BD
        y -= 10
        write(
            f"Base de datos: {'sí' if config['use_db'] else 'no'} (solo informativo)",
            40,
            20,
        )
        write(
            "Recuerdo que tengo que crear los secrets en GitHub → Settings → Secrets and variables → Actions.",
            40,
            20,
        )

        # Instrucciones detalladas
        y -= 20
        write("¿De dónde saco estos valores?", 40, 20)

        if config["use_sonar"]:
            write("Token de SonarCloud (SONAR_TOKEN):", 40, 18)
            write("1. Entro en SonarCloud con mi usuario.", 60)
            write("2. Arriba a la derecha → My Account → Security.", 60)
            write("3. Creo un token nuevo (scope: analysis).", 60)
            write("4. Copio el token y lo guardo (solo se muestra una vez).", 60)
            write("5. Creo el secret SONAR_TOKEN en GitHub con ese valor.", 60)
            y -= 10

        if config["use_aws"]:
            write("Credenciales de AWS (Access key / Secret key):", 40, 18)
            write("1. Entro en AWS Console → IAM → Users.", 60)
            write("2. Selecciono un usuario con permisos para ECR/EC2.", 60)
            write("3. Pestaña 'Security credentials' → Create access key.", 60)
            write("4. Copio 'Access key ID' y 'Secret access key'.", 60)
            access_name = config["aws_secrets"].get("access_key", "AWS_ACCESS_KEY_ID")
            secret_name = config["aws_secrets"].get(
                "secret_key", "AWS_SECRET_ACCESS_KEY"
            )
            write(
                f"5. Creo los secrets {access_name} y {secret_name} en GitHub.",
                60,
            )
            y -= 10

            write("Otros valores AWS necesarios:", 40, 18)
            region_name = config["aws_secrets"].get("region", "AWS_REGION")
            reg_name = config["aws_secrets"].get("ecr_registry", "AWS_ECR_URL")
            repo_name = config["aws_secrets"].get("ecr_repo", "ECR_REPOSITORY")
            host_name = config["aws_secrets"].get("ec2_host", "EC2_HOST")
            user_name = config["aws_secrets"].get("ec2_user", "EC2_USUARIO")

            write(f"- {region_name}: ej. eu-west-1", 60)
            write(
                f"- {reg_name}: copiar desde AWS ECR (Copy URI, parte del registry).",
                60,
            )
            write(
                f"- {repo_name}: nombre del repo de ECR (sin la URL completa).",
                60,
            )
            write(
                f"- {host_name}: IP pública / DNS de la instancia EC2.",
                60,
            )
            write(f"- {user_name}: ej. ubuntu / ec2-user.", 60)
            y -= 10

        write("Cómo crear secrets en GitHub:", 40, 18)
        write("1. Abro mi repositorio en GitHub.", 60)
        write("2. Voy a Settings → Secrets and variables → Actions.", 60)
        write("3. Pulso 'New repository secret'.", 60)
        write("4. Escribo el NOMBRE EXACTO del secret que aparece en este PDF.", 60)
        write("5. Pego el valor correspondiente.", 60)
        write("6. Repito el proceso para cada secret necesario.", 60)

        c.showPage()
        c.save()

    # -------------------------- RUN -------------------------- #
    def run(self) -> None:
        # Pido todos los datos, genero el YAML y el PDF y dejo un pequeño resumen por consola
        config = self.ask_inputs()
        yaml_content = self.render_yaml(config)
        yaml_path, pdf_path = self.output_paths()

        self.save_yaml(yaml_content, yaml_path)
        self._create_pdf(config, pdf_path)

        print("\nWorkflow de GitHub generado correctamente.")
        print(f" - YAML: {yaml_path}")
        print(f" - PDF:  {pdf_path}")

        print("\nResumen de configuración para GitHub (secrets y variables):")

        if config["use_sonar"]:
            print("\n1) Secrets para SonarCloud")
            print("   - SONAR_HOST_URL")
            print("     Ejemplo de valor: https://sonarcloud.io")
            print("   - SONAR_TOKEN")
            print("     Origen: SonarCloud → My Account → Security → nuevo token.")

        if config["use_aws"]:
            print("\n2) Secrets para AWS (ECR + EC2)")
            aws = config["aws_secrets"]
            print(
                f"   - {aws.get('access_key', 'AWS_ACCESS_KEY_ID')}  (Access Key ID IAM)"
            )
            print(
                f"   - {aws.get('secret_key', 'AWS_SECRET_ACCESS_KEY')}  (Secret Access Key IAM)"
            )
            print(f"   - {aws.get('region', 'AWS_REGION')}  (Región, ej: eu-west-1)")
            print(
                f"   - {aws.get('ecr_registry', 'AWS_ECR_URL')}  (Registry ECR, ej: 123456789012.dkr.ecr.eu-west-1.amazonaws.com)"
            )
            print(
                f"   - {aws.get('ecr_repo', 'ECR_REPOSITORY')}  (Nombre del repo ECR, ej: tfg-cicd-aws-2526)"
            )
            print(
                f"   - {aws.get('ec2_host', 'EC2_HOST')}  (DNS/IP pública de la EC2)"
            )
            print(
                f"   - {aws.get('ec2_user', 'EC2_USUARIO')}  (Usuario SSH, ej: ubuntu / ec2-user)"
            )

            print("\n   De dónde saco cada valor:")
            print("   - Credenciales IAM: AWS Console → IAM → Users → Security credentials.")
            print("   - Registry/Repo ECR: AWS Console → ECR → Repositories → Copy URI.")
            print(
                "   - Host EC2: AWS Console → EC2 → Instances → Public IPv4 DNS / address."
            )
            print("   - Usuario EC2: depende de la AMI (por ejemplo, 'ubuntu').")

        if config["use_sonar"] or config["use_aws"]:
            print("\nDónde crear los secrets:")
            print("   1. Entro en GitHub y abro mi repositorio.")
            print("   2. Voy a Settings → Secrets and variables → Actions.")
            print("   3. Creo un 'New repository secret' para cada nombre que he usado.")