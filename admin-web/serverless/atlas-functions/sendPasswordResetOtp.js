/**
 * MongoDB Atlas App Services Function: sendPasswordResetOtp
 * 
 * Sends a 6-digit transactional email OTP for password recovery via Resend API.
 * Updates the user's document in the `engineers` collection with the OTP and 10-minute expiry.
 * 
 * Context: Atlas App Services (Node.js runtime)
 * Secrets required in Atlas App Services:
 * - RESEND_API_KEY
 */

exports = async function({ username, email }) {
  const mongodb = context.services.get("mongodb-atlas");
  const db = mongodb.db("shift_roster");
  const engineersColl = db.collection("engineers");

  // 1. Locate engineer record by username or email
  const query = username ? { username: username.toLowerCase().trim() } : { email: email.toLowerCase().trim() };
  const engineer = await engineersColl.findOne(query);

  if (!engineer) {
    return {
      success: false,
      error: "No engineer profile found matching the provided identifier."
    };
  }

  // 2. Generate secure 6-digit numeric OTP
  const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 minutes from now

  // 3. Persist OTP in MongoDB Atlas engineer profile
  await engineersColl.updateOne(
    { _id: engineer._id },
    {
      $set: {
        reset_otp: {
          code: otpCode,
          expires_at: expiresAt,
          created_at: new Date()
        }
      }
    }
  );

  // 4. Send email via Resend API
  const resendApiKey = context.values.get("RESEND_API_KEY");
  if (!resendApiKey) {
    throw new Error("RESEND_API_KEY is not configured in Atlas values.");
  }
  const recipientEmail = engineer.email;

  try {
    const response = await context.http.post({
      url: "https://api.resend.com/emails",
      headers: {
        "Authorization": [`Bearer ${resendApiKey}`],
        "Content-Type": ["application/json"]
      },
      body: JSON.stringify({
        from: "RosterX NOC <no-reply@rosterx.dev>",
        to: [recipientEmail],
        subject: "RosterX: 6-Digit Password Reset Verification Code",
        html: `
          <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background: #ffffff;">
            <div style="border-bottom: 2px solid #3b82f6; padding-bottom: 12px; margin-bottom: 20px;">
              <h2 style="margin: 0; color: #0f172a; font-size: 20px; font-weight: 700; letter-spacing: -0.5px;">RosterX — NOC Operations Center</h2>
              <p style="margin: 4px 0 0; color: #64748b; font-size: 13px;">Security & Authentication Service</p>
            </div>
            <p style="color: #334155; font-size: 14px; line-height: 1.6;">Hello <strong>${engineer.name}</strong>,</p>
            <p style="color: #334155; font-size: 14px; line-height: 1.6;">A password reset request was initiated for your RosterX account (<code>${engineer.username}</code>). Enter the verification code below to authorize your password update:</p>
            
            <div style="background: #f1f5f9; border-radius: 8px; padding: 18px; text-align: center; margin: 24px 0;">
              <span style="font-family: 'Courier New', Courier, monospace; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #1e293b; display: inline-block;">${otpCode}</span>
              <p style="margin: 8px 0 0; font-size: 12px; color: #64748b; font-weight: 500;">Valid for 10 minutes (expires at ${expiresAt.toISOString().slice(11, 16)} UTC)</p>
            </div>

            <p style="color: #64748b; font-size: 12px; line-height: 1.5;">If you did not request this OTP, please contact the Lead Shift Admin or ignore this email. No changes will be made without this code.</p>
            <div style="border-top: 1px solid #f1f5f9; margin-top: 24px; padding-top: 12px; font-size: 11px; color: #94a3b8; text-align: center;">
              RosterX 24/7 NOC Shift System &bull; Automated Security Dispatch
            </div>
          </div>
        `
      })
    });

    return {
      success: true,
      message: `Verification code dispatched to ${recipientEmail}`,
      engineer_id: engineer._id,
      expires_at: expiresAt.toISOString()
    };
  } catch (err) {
    return {
      success: false,
      error: `Failed to dispatch email: ${err.message}`
    };
  }
};
