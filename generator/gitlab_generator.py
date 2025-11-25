from pathlib import Path

from jinja2 import Environment, FileSystemLoader
from reportlab.pdfgen import canvas


class GitLabWorkflowGenerator:
    """
    Generador de .gitlab-ci.yml:
    - Pide datos básicos, Sonar y AWS/ECR/EC2
    - Renderiza templates/gitlab_ci.yml.j2
    - Crea generator/workflow-gitlab.pdf
    """

    def __init__(self) -> None:
        self.root = Path(__file__).resolve().parents[1]
        self.templates_dir = self.root / "generator" / "templates"

    # -------------------------- INPUTS -------------------------- #
    def ask_inputs(self) -> dict:
        print("=== Generador GitLab CI/CD ===\n")

        project_name = input("Nombre del proyecto [mi-proyecto]: ").strip() or "mi-proyecto"

        branches_raw = input(
            "Ramas donde quieres que se ejecute CI (ej: main,develop) [main]: "
        ).strip()
        branches = (
            ["main"]
            if not branches_raw
            else [b.strip() for b in branches_raw.split(",") if b.strip()]
        )

        run_on_mr = (
                            input("¿Ejecutar pipeline en Merge Requests? (s/n) [s]: ").strip().lower()
                            or "s"
                    ) == "s"

        # ---------------- SONAR ----------------
        use_sonar = (
                            input("¿Quieres usar SonarCloud? (s/n) [s]: ").strip().lower() or "s"
                    ) == "s"

        sonar = {"host": "", "project_key": "", "organization": ""}

        if use_sonar:
            sonar["host"] = (
                    input("URL SonarCloud [https://sonarcloud.io]: ").strip()
                    or "https://sonarcloud.io"
            )
            sonar["project_key"] = input("sonar.projectKey: ").strip()
            sonar["organization"] = input("sonar.organization: ").strip()

        # ---------------- AWS / ECR / EC2 ----------------
        use_aws = (
                          input(
                              "¿Quieres añadir build+push a ECR + deploy en EC2? (s/n) [n]: "
                          ).strip().lower()
                          or "n"
                  ) == "s"

        # Importante: aquí NO pedimos los valores secretos, solo
        # cómo vamos a referirnos a las variables ya creadas en GitLab.
        aws = {
            # Nombres de variables CI/CD que ya tienes en GitLab
            "access_key_var": "AWS_ACCESS_KEY_ID",
            "secret_key_var": "AWS_SECRET_ACCESS_KEY",
            "region_var": "AWS_REGION",
            "registry": "",
            "repository": "",
            "ec2_host_var": "EC2_HOST",
            "ec2_user_var": "EC2_USER",
            "ssh_key_var": "EC2_SSH_KEY",
            "known_hosts_var": "EC2_KNOWN_HOSTS",
        }

        if use_aws:
            print(
                "\nPara GitLab se recomienda guardar estos valores como CI/CD variables "
                "en Settings → CI/CD → Variables.\n"
                "En tu proyecto ya las tienes creadas (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, "
                "AWS_REGION, AWS_ECR_URL, ECR_REPOSITORY, EC2_HOST, EC2_USER, EC2_SSH_KEY, "
                "EC2_KNOWN_HOSTS). Aquí solo elegimos cómo usarlas en el YAML."
            )

            # Normalmente usarás directamente las variables existentes
            aws["registry"] = (
                    input(
                        "ECR registry (normalmente $AWS_ECR_URL) [$AWS_ECR_URL]: "
                    ).strip()
                    or "$AWS_ECR_URL"
            )
            aws["repository"] = (
                    input(
                        "ECR repository name (normalmente $ECR_REPOSITORY) [$ECR_REPOSITORY]: "
                    ).strip()
                    or "$ECR_REPOSITORY"
            )

        return {
            "platform": "gitlab",
            "project_name": project_name,
            "branches": branches,
            "run_on_mr": run_on_mr,
            "use_sonar": use_sonar,
            "sonar": sonar,
            "use_aws": use_aws,
            "aws": aws,
        }

    # -------------------------- RENDER -------------------------- #
    def render_yaml(self, config: dict) -> str:
        env = Environment(loader=FileSystemLoader(str(self.templates_dir)))
        template = env.get_template("gitlab_ci.yml.j2")
        return template.render(config=config)

    def output_paths(self) -> tuple[Path, Path]:
        yaml_path = self.root / ".gitlab-ci.yml"
        pdf_path = self.root / "generator" / "workflow-gitlab.pdf"
        return yaml_path, pdf_path

    @staticmethod
    def save_yaml(content: str, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)

    # -------------------------- PDF -------------------------- #
    @staticmethod
    def _create_pdf(config: dict, pdf_path: Path) -> None:
        """
        PDF explicativo:
        - Resumen configuración
        - Qué variables CI/CD intervienen
        - Dónde conseguir cada valor (IAM, ECR, EC2, SonarCloud)
        - Dónde crear las variables en GitLab
        """
        c = canvas.Canvas(str(pdf_path))
        y = 800

        def write(text: str, indent: int = 40, step: int = 15):
            nonlocal y
            if y < 60:
                c.showPage()
                y = 800
            c.drawString(indent, y, text)
            y -= step

        # Encabezado
        write("Resumen pipeline GitLab CI/CD generado", 40, 30)
        write(f"Proyecto: {config['project_name']}")
        write("Plataforma CI: GitLab CI/CD")
        ramas_txt = ", ".join(config["branches"]) if config["branches"] else "-"
        write(f"Ramas CI: {ramas_txt}")
        write(
            f"Pipeline en Merge Requests: {'sí' if config['run_on_mr'] else 'no'}",
            40,
            20,
        )

        # Sonar
        write(f"Sonar: {'sí' if config['use_sonar'] else 'no'}", 40, 20)
        if config["use_sonar"]:
            if config["sonar"].get("host"):
                write(f"Host Sonar: {config['sonar']['host']}")
            if config["sonar"].get("project_key"):
                write(f"sonar.projectKey: {config['sonar']['project_key']}")
            if config["sonar"].get("organization"):
                write(f"sonar.organization: {config['sonar']['organization']}")
            y -= 10

        # AWS / ECR / EC2
        write(f"AWS/ECR/EC2: {'sí' if config['use_aws'] else 'no'}", 40, 20)
        if config["use_aws"]:
            aws = config["aws"]
            write(f"ECR Registry (YAML): {aws['registry']}", 40, 20)
            write(f"ECR Repository (YAML): {aws['repository']}", 40, 20)
            write("Variables CI/CD utilizadas:", 40, 20)
            write(f"- {aws['access_key_var']}  (access key IAM)", 60)
            write(f"- {aws['secret_key_var']}  (secret key IAM)", 60)
            write(f"- {aws['region_var']}      (región AWS)", 60)
            write(f"- AWS_ECR_URL              (URL del registry ECR)", 60)
            write(f"- ECR_REPOSITORY           (nombre del repo ECR)", 60)
            write(f"- {aws['ec2_host_var']}    (IP/DNS de la EC2)", 60)
            write(f"- {aws['ec2_user_var']}    (usuario SSH de la EC2)", 60)
            write(f"- {aws['ssh_key_var']}     (clave SSH privada .pem)", 60)
            write(f"- {aws['known_hosts_var']} (huella SSH opcional)", 60)
            y -= 10

        # Dónde crear las variables en GitLab
        write("¿Dónde crear las variables en GitLab?", 40, 20)
        write("1. Ve a tu proyecto en GitLab.", 60)
        write("2. Menú lateral → Settings → CI/CD.", 60)
        write("3. En 'Variables', pulsa 'Expand'.", 60)
        write("4. Pulsa 'Add variable'.", 60)
        write("5. En 'Key' pon el NOMBRE: p.ej. AWS_ACCESS_KEY_ID.", 60)
        write("6. En 'Value' pega el valor real (access key, secret, etc.).", 60)
        write("7. Marca 'Protected' para usarla solo en ramas protegidas.", 60)
        write("8. Marca 'Masked' para que no salga en los logs (si es secreto).", 60)

        # De dónde sacar cada valor AWS
        if config["use_aws"]:
            y -= 20
            write("¿De dónde saco cada valor de AWS?", 40, 18)

            write("AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY:", 60)
            write(" - AWS Console → IAM → Users.", 80)
            write(" - Selecciona el usuario de despliegue.", 80)
            write(" - Pestaña 'Security credentials' → Create access key.", 80)

            write("AWS_REGION:", 60)
            write(" - Región donde tienes ECR/EC2 (ej: eu-west-1).", 80)

            write("AWS_ECR_URL:", 60)
            write(" - AWS Console → ECR → Repositories.", 80)
            write(" - En tu repo, botón 'Copy URI'.", 80)

            write("ECR_REPOSITORY:", 60)
            write(" - Es el NOMBRE del repositorio en ECR (sin la URL).", 80)

            write("EC2_HOST:", 60)
            write(" - AWS Console → EC2 → Instances.", 80)
            write(" - Columna 'Public IPv4 address' o 'Public DNS'.", 80)

            write("EC2_USER:", 60)
            write(" - Depende de la AMI: normalmente 'ubuntu' o 'ec2-user'.", 80)

            write("EC2_SSH_KEY:", 60)
            write(" - Contenido de tu .pem de la EC2 (sin passphrase).", 80)

            write("EC2_KNOWN_HOSTS (opcional):", 60)
            write(" - Salida de 'ssh-keyscan -H <EC2_HOST>' en tu terminal.", 80)

        # SonarCloud en GitLab
        if config["use_sonar"]:
            y -= 20
            write("Token SonarCloud para GitLab (SONAR_TOKEN):", 40, 18)
            write("1. Entra en SonarCloud con tu usuario.", 60)
            write("2. Menú usuario → 'My Account' → 'Security'.", 60)
            write("3. Crea un token nuevo y cópialo.", 60)
            write("4. En GitLab crea la variable SONAR_TOKEN con ese valor.", 60)

        c.showPage()
        c.save()

    # -------------------------- RUN -------------------------- #
    def run(self) -> None:
        config = self.ask_inputs()
        yaml_content = self.render_yaml(config)
        yaml_path, pdf_path = self.output_paths()

        self.save_yaml(yaml_content, yaml_path)
        self._create_pdf(config, pdf_path)

        print("\n✅ Pipeline de GitLab generado correctamente:")
        print(f" - YAML: {yaml_path}")
        print(f" - PDF:  {pdf_path}")