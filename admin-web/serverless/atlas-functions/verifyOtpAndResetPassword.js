/**
 * MongoDB Atlas App Services Function: verifyOtpAndResetPassword
 * 
 * Verifies the 6-digit OTP against the expiration threshold (10 minutes)
 * Hashes the new PIN / password using SHA-256
 * Updates the engineer document in the `engineers` collection and invalidates the used OTP.
 * 
 * Context: Atlas App Services (Node.js runtime with crypto module)
 */

const crypto = require("crypto");

exports = async function({ username, email, otp, newPin }) {
  const mongodb = context.services.get("mongodb-atlas");
  const db = mongodb.db("shift_roster");
  const engineersColl = db.collection("engineers");

  // 1. Validation of input parameters
  if ((!username && !email) || !otp || !newPin) {
    return {
      success: false,
      error: "Missing required parameters: username/email, otp, and newPin are required."
    };
  }

  // 2. Find engineer profile
  const query = username ? { username: username.toLowerCase().trim() } : { email: email.toLowerCase().trim() };
  const engineer = await engineersColl.findOne(query);

  if (!engineer) {
    return {
      success: false,
      error: "Engineer profile not found."
    };
  }

  // 3. Verify OTP state
  const resetOtp = engineer.reset_otp;
  if (!resetOtp || !resetOtp.code) {
    return {
      success: false,
      error: "No active password reset request found. Please request a new code."
    };
  }

  // Check code match
  if (resetOtp.code.trim() !== otp.trim()) {
    return {
      success: false,
      error: "Invalid 6-digit verification code. Please check and try again."
    };
  }

  // Check 10-minute expiration
  const expiresAt = new Date(resetOtp.expires_at).getTime();
  const now = Date.now();
  if (now > expiresAt) {
    return {
      success: false,
      error: "Verification code has expired (exceeded 10 minutes limit). Please request a new one."
    };
  }

  // 4. Hash new password/PIN with SHA-256
  const pinHash = crypto.createHash("sha256").update(String(newPin).trim()).digest("hex");

  // 5. Update engineer document in MongoDB Atlas & invalidate OTP
  await engineersColl.updateOne(
    { _id: engineer._id },
    {
      $set: {
        pin_hash: pinHash,
        updated_at: new Date()
      },
      $unset: {
        reset_otp: ""
      }
    }
  );

  return {
    success: true,
    message: "Password/PIN successfully updated. You may now log in.",
    engineer_id: engineer._id,
    updated_at: new Date().toISOString()
  };
};
