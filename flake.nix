{
  description = "Basic flake and files - change me";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, ... }@inputs:
    inputs.flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        # nix fmt formatter
        formatter = nixpkgs.legacyPackages.${system}.nixpkgs-fmt;

        # default devshell
        devShells.default = nixpkgs.legacyPackages.${system}.mkShell {
          packages = [
            pkgs.pre-commit
            pkgs.go-task
            pkgs.fluxcd
            pkgs.kubeconform
            pkgs.kustomize
            pkgs.yq-go
          ];

          # Will be executed before entering the shell
          # or running a command
          shellHook = ''
          '';
        };
      }
    );
}
