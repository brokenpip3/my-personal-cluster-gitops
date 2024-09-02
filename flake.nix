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
        validationpkgs = [ pkgs.kubeconform pkgs.kustomize pkgs.yq-go ];
      in
      {
        formatter = nixpkgs.legacyPackages.${system}.nixpkgs-fmt;

        devShells.default = nixpkgs.legacyPackages.${system}.mkShell {
          packages = with pkgs; [
            pre-commit
            go-task
            fluxcd
            validationpkgs
            (writeShellApplication {
              name = "validate";
              runtimeInputs = validationpkgs;
              text = builtins.readFile ./scripts/github/validate.sh;
            })
          ];

          shellHook = ''
          '';
        };
      }
    );
}
