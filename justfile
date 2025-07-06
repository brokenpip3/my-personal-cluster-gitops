flake-check:
    # Check whether the flake evaluates and run its tests
    nix flake check

flake-update-all:
    # Update all flake inputs
    printf "\n> Updating all flake inputs\n\n"
    nix flake update

flake-update-one ARG:
    # Update one flake input like nixpkgs or flake-utils
    printf "\n> Updating flake input {{ARG}}\n\n"
    nix flake lock --update-input {{ARG}}

clean-git:
    # Clean git reflog
    git reflog expire --verbose --expire-unreachable=now --all
    git gc --prune=now

pre-commit:
    # Initialize pre-commit
    pre-commit install

tests:
    # All tests
    echo 'test'

validate:
    # Validate flux and k8s resources
    util_repo_my_cluster_gitops_validate

outdated:
    # Check latest helm charts
    util_repo_my_cluster_gitops_outdated
