module "google_cloud_dcos" {
  source = "github.com/lavrov/terraform-dcos/gcp"

  num_of_masters = "1"
  num_of_private_agents = "3"
  num_of_public_agents = "0"
  #
  gcp_project = "ferrous-amphora-119419"
  gcp_region = "us-central1"
  # If you want to use GCP service account key instead of GCP SDK
  # uncomment the line below and update it with the path to the key file
  gcp_credentials_key_file = "/Users/vitaly/workspace/my/cloud/google/aired-key.json"
  gcp_ssh_pub_key_file = "/Users/vitaly/.ssh/id_rsa.pub"
  #
  gcp_bootstrap_instance_type = "f1-micro"
  gcp_master_instance_type = "n1-standard-1"
  gcp_agent_instance_type = "n1-standard-2"
  gcp_public_agent_instance_type = "n1-standard-2"
  #
  # Change public/private subnetworks e.g. "10.65." if you want to run multiple clusters in the same project
  #gcp_compute_subnetwork_public = "10.64.0.0/22"
  #gcp_compute_subnetwork_private = "10.64.4.0/22"
  # Inbound Master Access
  admin_cidr = "0.0.0.0/0"

  # Uncomment the line below if you want short living cheap cluster for testing
  #gcp_scheduling_preemptible = "true"
}
