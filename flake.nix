{
  description = "My cluster gitops";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flux_2_8.url = "github:nixos/nixpkgs/e607cb5360ff1234862ac9f8839522becb853bb9";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    { self, nixpkgs, ... }@inputs:
    inputs.flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        flux = inputs.flux_2_8.legacyPackages.${system}.fluxcd;
        flux-operator = inputs.flux_2_8.legacyPackages.${system}.fluxcd-operator;
        validationpkgs = [
          pkgs.kubeconform
          pkgs.kustomize
          pkgs.yq-go
        ];
      in
      {
        formatter = nixpkgs.legacyPackages.${system}.nixpkgs-fmt;

        devShells.default = nixpkgs.legacyPackages.${system}.mkShell {
          packages = with pkgs; [
            flux
            flux-operator
            grafana-loki
            just
            kubectl-view-secret
            minio-client
            nova
            pre-commit
            renovate
            sops
            validationpkgs
            yq-go
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
              name = "util_repo_my_cluster_gitops_logcli";
              runtimeInputs = with pkgs; [
                kubectl
                grafana-loki
              ];
              text = ''
                query="$1"
                kubectl port-forward svc/loki -n loki 3100:3100 >/dev/null 2>&1 &
                _PID=$!
                sleep 1
                ${pkgs.grafana-loki}/bin/logcli "$query"
                kill $_PID
              '';
            })
            (writeShellApplication {
              name = "util_repo_my_cluster_gitops_prometheus_open";
              runtimeInputs = with pkgs; [
                kubectl
              ];
              text = ''
                sleep 2 && xdg-open http://localhost:9090 &
                kubectl port-forward svc/prom-kube-prometheus-stack-prometheus -n monitoring 9090
              '';
            })
          ];

          shellHook = ''
            printf "\nrepo utils:\n\n"
            compgen -c util_repo_my_cluster_gitops_
          '';
        };
      }
    );
}
