{
  description = "Basic flake and files - change me";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flux_2_3.url = "github:nixos/nixpkgs/3281bec7174f679eabf584591e75979a258d8c40";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, ... }@inputs:
    inputs.flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        flux = inputs.flux_2_3.legacyPackages.${system}.fluxcd;
        validationpkgs = [ pkgs.kubeconform pkgs.kustomize pkgs.yq-go ];
      in
      {
        formatter = nixpkgs.legacyPackages.${system}.nixpkgs-fmt;

        devShells.default = nixpkgs.legacyPackages.${system}.mkShell {
          packages = with pkgs; [
            pre-commit
            go-task
            flux
            nova
            validationpkgs
            (writeShellApplication {
              name = "validate";
              runtimeInputs = validationpkgs;
              text = builtins.readFile ./scripts/github/validate.sh;
            })
            (writeShellApplication {
              name = "outdated";
              runtimeInputs = with pkgs; [ nova ];
              text = ''
                nova find --helm --format table
              '';
            })
          ];

          shellHook = ''
          '';
        };
      }
    );
}
