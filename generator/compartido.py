from __future__ import annotations

from pathlib import Path
from typing import Dict

from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4


# Aquí tengo el formulario común que uso tanto para GitHub como para GitLab.
def ask_common_ci_inputs(ci_platform: str) -> Dict:
    """
    Preguntas comunes para GitHub y GitLab:
    - SonarCloud
    - AWS (ECR + EC2)
    - doy por hecho que las variables DB_* ya existen como secrets/variables en el repo.
    """
    print("\n=== Configuración común CI/CD (SonarCloud, AWS) ===\n")

    # PARTE DE SONAR
    # Primero pregunto si quiero activar SonarCloud en la pipeline.
    use_sonar = (
                        input("¿Incluir análisis SonarCloud? (s/n) [s]: ").strip().lower() or "s"
                ) == "s"

    fail_on_sonar = False
    if use_sonar:
        # Aquí decido si quiero que la pipeline falle cuando el Quality Gate no pasa.
        fail_on_sonar = (
                                input(
                                    "Si falla el Quality Gate de Sonar, ¿quiero que se ROMPA la pipeline? "
                                    "(s/n) [n]: "
                                ).strip().lower()
                                or "n"
                        ) == "s"

    # PARTE DE AWS
    # Ahora pregunto si quiero añadir el despliegue completo a AWS (ECR + EC2).
    use_aws = (
                      input("¿Añadir deploy completo a AWS (ECR + EC2)? (s/n) [n]: ")
                      .strip()
                      .lower()
                      or "n"
              ) == "s"

    deploy_mode = "none"
    aws_secrets: Dict[str, str] = {}

    if use_aws:
        print("\n¿Cuándo quiero lanzar el deploy?")
        print(" 1) En cada push a main")
        print(" 2) Solo con tag (release)")
        print(" 3) Solo manual (desde la UI de CI/CD)")
        opcion = input("Elige 1/2/3 [2]: ").strip() or "2"
        # Mapeo la opción a un modo de despliegue interno.
        deploy_mode = {"1": "main", "2": "tag", "3": "manual"}.get(opcion, "tag")

        # adaptado el mensaje según la plataforma donde vaya a configurar las variables.
        if ci_platform == "github":
            print(
                "\nPara AWS voy a usar NOMBRES de secrets de GitHub.\n"
                "Luego tendré que crear esos secrets con sus valores reales en GitHub."
            )
        else:
            print(
                "\nPara AWS voy a usar NOMBRES de variables de GitLab CI/CD.\n"
                "Luego tendré que crear esas variables con sus valores reales en GitLab."
            )

        # Aquí decido si quiero usar los nombres estándar o personalizarlos.
        usar_defectos = (
                                input(
                                    "¿Usar nombres estándar "
                                    "(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, EC2_LLAVE_SSH, etc.)? "
                                    "(s/n) [s]: "
                                ).strip().lower()
                                or "s"
                        ) == "s"

        if usar_defectos:
            # Si tiro por defecto, dejo todos los nombres típicos que suelo usar.
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
            # Si no uso los por defecto, aquí puedo personalizar cada nombre.
            print(
                "\nIntroduzco los NOMBRES de los secrets/variables (no sus valores). "
                "Si dejo algo vacío, uso el nombre por defecto entre paréntesis."
            )
            aws_secrets = {
                "access_key": input(
                    "Nombre secret/variable para AWS access key id [AWS_ACCESS_KEY_ID]: "
                ).strip()
                              or "AWS_ACCESS_KEY_ID",
                "secret_key": input(
                    "Nombre secret/variable para AWS secret access key "
                    "[AWS_SECRET_ACCESS_KEY]: "
                ).strip()
                              or "AWS_SECRET_ACCESS_KEY",
                "region": input(
                    "Nombre secret/variable para región AWS [AWS_REGION]: "
                ).strip()
                          or "AWS_REGION",
                "ecr_registry": input(
                    "Nombre secret/variable para URL del registry ECR [AWS_ECR_URL]: "
                ).strip()
                                or "AWS_ECR_URL",
                "ecr_repo": input(
                    "Nombre secret/variable para nombre del repositorio ECR "
                    "[ECR_REPOSITORY]: "
                ).strip()
                            or "ECR_REPOSITORY",
                "ec2_host": input(
                    "Nombre secret/variable para host/IP de EC2 [EC2_HOST]: "
                ).strip()
                            or "EC2_HOST",
                "ec2_user": input(
                    "Nombre secret/variable para usuario de EC2 [EC2_USUARIO]: "
                ).strip()
                            or "EC2_USUARIO",
                "ec2_key": input(
                    "Nombre secret/variable para la clave SSH (.pem) de EC2 "
                    "[EC2_LLAVE_SSH]: "
                ).strip()
                           or "EC2_LLAVE_SSH",
            }

    # Aquí paso todo lo necesario para generar las plantillas.
    return {
        "use_sonar": use_sonar,
        "fail_on_sonar": fail_on_sonar,
        "use_aws": use_aws,
        "deploy_mode": deploy_mode,
        "aws_secrets": aws_secrets,
        "ci_platform": ci_platform,
    }


# Aquí tengo la parte común que genera el PDF para GitHub y GitLab.
def create_common_pdf(config: Dict, pdf_path: Path) -> None:
    # miro si estoy generando para GitHub o GitLab solo para adaptar algunos textos.
    ci_platform = config.get("ci_platform", "github")
    is_github = ci_platform == "github"
    plataforma_txt = "GitHub Actions" if is_github else "GitLab CI/CD"

    # Configuración básica del PDF.
    c = canvas.Canvas(str(pdf_path), pagesize=A4)
    width, height = A4
    y = height - 50  # Empiezo a escribir un poco por debajo del borde superior.

    use_sonar = config.get("use_sonar", False)
    use_aws = config.get("use_aws", False)

    # Para el TFG asumo siempre que hay BD y que las variables DB_* existen.
    use_db = True

    aws_secrets_cfg: Dict = config.get("aws_secrets") or {}

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

    def wrap_text(text: str, max_width: int = 90):
        # Pequeño ajuste para que las líneas de texto no se salgan horizontalmente.
        words = text.split()
        lines = []
        current = []
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

    def paragraph(text: str, indent: int = 40, step: int = 14, bold: bool = False):
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
        # conjunto de código común para documentar cada secret/variable en el PDF.
        nonlocal y
        if y < 120:
            new_page()
        line()
        paragraph(f"Nombre del secret/variable: {nombre}", indent=45, bold=True)
        paragraph(f"Ejemplo de valor: {ejemplo}", indent=60)
        paragraph(f"¿De dónde saco este valor?: {donde}", indent=60)
        y -= 4
        line()
        y -= 4

    #  cabecera
    title(f"Resumen del workflow generado para {plataforma_txt}")

    ramas_txt = ", ".join(config.get("branches", [])) or "-"
    paragraph(f"Proyecto: {config.get('project_name', '-')}")
    paragraph(f"Ramas donde corre la CI: {ramas_txt}")

    if is_github:
        paragraph(
            "¿Ejecuta en pull_request?: "
            f"{'sí' if config.get('run_on_pr') else 'no'}"
        )
        paragraph(
            "¿Proyecto con Node?: "
            f"{'sí' if config.get('use_node') else 'no'}"
        )
    else:
        # En GitLab, de momento no genero jobs con Node en este proyecto, ya que no tiene
        paragraph("¿Proyecto con Node?: no ")

    paragraph(f"¿Incluye SonarCloud?: {'sí' if use_sonar else 'no'}")
    paragraph(f"¿Incluye deploy a AWS (ECR + EC2)?: {'sí' if use_aws else 'no'}")
    paragraph(f"¿Usa base de datos?: {'sí' if use_db else 'no'}")
    line()

    #  SONAR
    if use_sonar:
        subtitle("Secrets/variables necesarios para SonarCloud")

        paragraph(
            "Variables necesarias para que el análisis de SonarCloud funcione "
            "dentro de la pipeline.",
            indent=40,
        )

        # Ejemplos de valores típicos para documentar en el PDF.
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
            "en este secret/variable.",
        )
        secret_block(
            "SONAR_ORGANIZATION",
            sonar_org,
            "En SonarCloud, arriba a la derecha, puedo ver el identificador de mi "
            "organización (Organization key). Copio ese valor y lo uso en este secret/"
            "variable.",
        )
        secret_block(
            "SONAR_TOKEN",
            "Token personal de análisis",
            "En SonarCloud entro con mi usuario, voy a My Account → Security, "
            "creo un token nuevo y copio el valor. Ese valor es el que pego "
            "en este secret/variable.",
        )

    # PARTE DE AWS
    if use_aws:
        subtitle("Secrets/variables necesarios para AWS (ECR + EC2)")

        paragraph(
            "Secrets/variables necesarios para poder hacer login en ECR, subir "
            "imágenes Docker y conectarme por SSH a la instancia EC2 donde despliego.",
            indent=40,
        )

        # Nombres por defecto que le pongo para las variables AWS.
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

        # Ejemplos de valores para que el PDF.
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

        # descripcion de donde busco cada valor de variable
        where_text = {
            "access_key": (
                "En AWS Console entro a IAM → Users, selecciono mi usuario y en la pestaña "
                "'Security credentials' creo una access key. Uso el valor de Access key ID."
            ),
            "secret_key": (
                "En el mismo sitio donde creo la access key (IAM → Users → Security "
                "credentials) copio el Secret access key. Ese valor solo se muestra una vez, "
                "así que lo guardo y lo pego en este secret/variable."
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
                "tal cual dentro de este secret/variable."
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

        # Recorro todas las variables de AWS en un orden fijo para el PDF.
        for key in ordered_keys:
            secret_name = aws_secrets_cfg.get(key) or default_secret_names[key]
            secret_block(secret_name, examples[key], where_text[key])

    # PARTE DE LA BASE DE DATOS
    if use_db:
        subtitle("Variables necesarias para Base de Datos")

        paragraph(
            "Estas variables permiten conectar la aplicación con cualquier base de datos "
            "externa (por ejemplo RDS en AWS) o interna (un contenedor Docker).",
            indent=40,
        )

        # Ejemplos de valores por defecto que uso en mi proyecto.
        engine = "postgresql"
        host = "postgres-db"
        port = "5432"
        name = "tfg"
        user = "tfg"
        password_example = "********"

        secret_block(
            "DB_ENGINE",
            engine,
            "Motor de base de datos. Ejemplos: postgresql, mysql, mariadb, sqlserver, oracle.",
        )
        secret_block(
            "DB_HOST",
            host,
            "Host o endpoint de la base de datos. Si usas Docker local: postgres-db. "
            "Si usas RDS: copia el endpoint desde AWS RDS.",
        )
        secret_block(
            "DB_PORT",
            port,
            "Puerto de conexión. Depende del motor: PostgreSQL=5432, MySQL/MariaDB=3306, "
            "SQL Server=1433, etc.",
        )
        secret_block(
            "DB_NAME",
            name,
            "Nombre de la base de datos. Lo defines al crear tu BD o lo lees del panel "
            "de RDS/gestor de BBDD.",
        )
        secret_block(
            "DB_USER",
            user,
            "Usuario con permisos para conectarse a la base de datos. En RDS lo defines "
            "al crear la instancia.",
        )
        secret_block(
            "DB_PASSWORD",
            password_example,
            "Password de acceso del usuario de base de datos. No se guarda en código, "
            "solo en secrets/variables.",
        )

    # PARTE DE Cómo crear secrets/variables
    if is_github:
        subtitle("Cómo creo los secrets en GitHub")

        paragraph("1. Entro en el repositorio de GitHub del proyecto.")
        paragraph("2. Voy a Settings → Secrets and variables → Actions.")
        paragraph("3. Pulso el botón 'New repository secret'.")
        paragraph(
            "4. En 'Name' escribo exactamente el nombre del secret que aparece "
            "en este documento (por ejemplo AWS_ACCESS_KEY_ID, EC2_LLAVE_SSH, "
            "SONAR_HOST_URL, SONAR_PROJECT_KEY, SONAR_ORGANIZATION, SONAR_TOKEN, "
            "DB_ENGINE, DB_HOST, etc.)."
        )
        paragraph(
            "5. En 'Secret' pego el valor real que he obtenido de AWS, SonarCloud "
            "o del proveedor de base de datos."
        )
        paragraph(
            "6. Repito estos pasos para cada uno de los secrets hasta tenerlos todos creados."
        )
    else:
        subtitle("Cómo creo las variables en GitLab")

        paragraph("1. Entro en el proyecto de GitLab del repositorio.")
        paragraph("2. Voy a Settings → CI/CD → Variables.")
        paragraph("3. Pulso el botón 'Add variable'.")
        paragraph(
            "4. En 'Key' escribo exactamente el nombre de la variable que aparece "
            "en este documento (por ejemplo AWS_ACCESS_KEY_ID, EC2_LLAVE_SSH, "
            "SONAR_HOST_URL, SONAR_PROJECT_KEY, SONAR_ORGANIZATION, SONAR_TOKEN, "
            "DB_ENGINE, DB_HOST, etc.)."
        )
        paragraph(
            "5. En 'Value' pego el valor real que he obtenido de AWS, SonarCloud "
            "o del proveedor de base de datos."
        )
        paragraph(
            "6. Marco 'Protected' y 'Masked' cuando corresponda y guardo la variable."
        )

    # Cierro y guardo el PDF en disco.
    c.save()