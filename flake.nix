{
  description = "Basic flake and files - change me";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flux_2_4.url = "github:nixos/nixpkgs/d4f247e89f6e10120f911e2e2d2254a050d0f732";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, ... }@inputs:
    inputs.flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        flux = inputs.flux_2_4.legacyPackages.${system}.fluxcd;
        validationpkgs = [ pkgs.kubeconform pkgs.kustomize pkgs.yq-go ];
      in
      {
        formatter = nixpkgs.legacyPackages.${system}.nixpkgs-fmt;

        devShells.default = nixpkgs.legacyPackages.${system}.mkShell {
          packages = with pkgs; [
            pre-commit
            just
            flux
            sops
            nova
            validationpkgs
            minio-client
            kubectl-view-secret
            grafana-loki
            (writeShellApplication {
              name = "util_repo_my_cluster_gitops_validate";
              runtimeInputs = validationpkgs;
              text = builtins.readFile ./scripts/github/validate.sh;
            })
            (writeShellApplication {
              name = "util_repo_my_cluster_gitops_outdated";
              runtimeInputs = with pkgs; [ nova ];
              text = ''
                nova find --helm --format table
              '';
            })
            (writeShellApplication {
              name = "util_logcli";
              runtimeInputs = with pkgs; [ kubectl grafana-loki ];
              text = ''
                kubectl port-forward svc/loki -n loki --context brokenpip3 3100:3100 >/dev/null 2>&1 &
                _PID=$!
                sleep 1
                logcli "$1"
                kill $_PID
              '';
            })
          ];

          shellHook = ''
          '';
        };
      }
    );
}
