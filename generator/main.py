from github_generator import GitHubWorkflowGenerator
from gitlab_generator import GitLabWorkflowGenerator


def ask_platform() -> str:
    print("=== Generador de Workflows CI/CD ===\n")
    platform = ""
    while platform not in ("github", "gitlab"):
        platform = (
            input("¿Para qué plataforma quieres generar CI/CD? (github/gitlab): ")
            .strip()
            .lower()
        )
    return platform


def main() -> None:
    platform = ask_platform()

    if platform == "github":
        generator = GitHubWorkflowGenerator()
        generator.run()
    else:
        generator = GitLabWorkflowGenerator()
        generator.run()


if __name__ == "__main__":
    main()