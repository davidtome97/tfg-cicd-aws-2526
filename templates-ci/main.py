from github_generator import GitHubWorkflowGenerator
from gitlab_generator import GitLabWorkflowGenerator


def ask_platform() -> str:
    print("=== Generador de Workflows CI/CD ===\n")
    platform = ""
    # Aquí pregunto al usuario para qué plataforma quiere generar la CI/CD.
    # Repito la pregunta hasta que me dé un valor válido.
    while platform not in ("github", "gitlab"):
        platform = (
            input("¿Para qué plataforma quieres generar CI/CD? (github/gitlab): ")
            .strip()
            .lower()
        )
    return platform


def main() -> None:
    # Primero pregunto qué plataforma ha elegido el usuario.
    platform = ask_platform()

    # Según la opción elegida, instancio el generador correspondiente.
    if platform == "github":
        generator = GitHubWorkflowGenerator()
        generator.run()
    else:
        generator = GitLabWorkflowGenerator()
        generator.run()


if __name__ == "__main__":
    # Ejecuto el main cuando lanzo este archivo directamente.
    main()

