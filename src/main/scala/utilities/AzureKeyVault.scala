package utils

import com.azure.identity.DefaultAzureCredentialBuilder
import com.azure.security.keyvault.secrets.SecretClientBuilder

object AzureKeyVault {

  /**
   * Resolve the client secret by first checking the environment.
   * If not found, fetch it from Azure Key Vault using the provided vault and secret name.
   */
  def loadClientSecret(vaultName: String, secretName: String): String = {
    sys.env.get("CLIENT_SECRET") match {
      case Some(secret) =>
        println("CLIENT_SECRET loaded from environment")
        secret
      case None =>
        println(s"CLIENT_SECRET not found in environment, fetching from Azure Key Vault: $vaultName / $secretName")
        getSecretFromKeyVault(vaultName, secretName)
    }
  }

  private def getSecretFromKeyVault(vaultName: String, secretName: String): String = {
    val vaultUrl = s"https://$vaultName.vault.azure.net"
    val client = new SecretClientBuilder()
      .vaultUrl(vaultUrl)
      .credential(new DefaultAzureCredentialBuilder().build())
      .buildClient()

    client.getSecret(secretName).getValue
  }
}
