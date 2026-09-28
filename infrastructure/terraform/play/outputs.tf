output "service_account_email" {
  description = "Invite this account in Play Console → Users and permissions (Release manager)"
  value       = google_service_account.play_publisher.email
}
