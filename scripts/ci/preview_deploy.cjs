// Shared state reconstruction and rendering for the preview deploy start/finish jobs.
// Callers select lifecycle status and reactions; hidden metadata remains the lock contract.
async function updateDeployComment({
    github, context, inputs, validatedSha = '', buildNumber = '',
    state, status, androidStatus, iosStatus, nextStep,
}) {
    const owner = context.repo.owner;
    const repo = context.repo.repo;
    const issueNumber = Number((inputs['pr-number'] || ''));
    const runUrl =
      `https://github.com/${owner}/${repo}` +
      `/actions/runs/${context.runId}`;
    const previewMarker =
      '<!-- Sticky Pull Request Commentpreview-build -->';
    const statusMarker = '<!-- preview-deploy-status -->';

    const readMeta = (body, key) => {
      const pattern = new RegExp(`<!-- ${key}:(.*?) -->`);
      return body.match(pattern)?.[1] ?? '';
    };

    const findStatusComment = (comments, targetRequestKey) =>
      comments.toReversed().find(
        (comment) =>
          comment.body?.includes(statusMarker) &&
          readMeta(comment.body, 'preview-deploy-request-key') ===
            targetRequestKey,
      );

    const resolvePreviewCommentUrl = ({
      comments,
      deploySha,
      fallbackUrl,
    }) => {
      const latestPreviewComment = comments.toReversed()
        .find((comment) => {
          if (!comment.body?.includes(previewMarker)) {
            return false;
          }

          if (!deploySha) {
            return true;
          }

          return (
            readMeta(comment.body, 'preview-build-source-sha') ===
            deploySha
          );
        });

      return latestPreviewComment?.html_url ?? fallbackUrl;
    };

    const comments = await github.paginate(
      github.rest.issues.listComments,
      {
        owner,
        repo,
        issue_number: issueNumber,
        per_page: 100,
      },
    );
    const requestCommentIdInput =
      (inputs['request-comment-id'] || '').trim();
    const requestKey = requestCommentIdInput
      ? `comment-${requestCommentIdInput}`
      : `workflow-${context.runId}`;
    const existing = findStatusComment(comments, requestKey);
    const existingBody = existing?.body ?? '';

    const requestedBy =
      (inputs['request-comment-user'] || '') ||
      readMeta(existingBody, 'preview-deploy-requested-by') ||
      context.actor;
    const requestedAt =
      (inputs['request-comment-at'] || '') ||
      readMeta(existingBody, 'preview-deploy-requested-at');
    const requestCommentId =
      requestCommentIdInput ||
      readMeta(existingBody, 'preview-deploy-last-command-comment-id');
    const requestCommentUrl =
      (inputs['request-comment-url'] || '') ||
      readMeta(existingBody, 'preview-deploy-request-comment-url');
    const requestedPreviewCommentUrl =
      (inputs['request-preview-comment-url'] || '') ||
      readMeta(existingBody, 'preview-deploy-preview-comment-url');
    const requestedPreviewBuildNumber =
      (inputs['preview-build-number'] || '') ||
      readMeta(existingBody, 'preview-deploy-requested-build-number');
    const deploySha =
      validatedSha ||
      readMeta(existingBody, 'preview-deploy-source-sha') ||
      (inputs['source-sha'] || '');
    const deployBuildNumber =
      buildNumber ||
      readMeta(existingBody, 'preview-deploy-build-number');
    const previewCommentUrl = resolvePreviewCommentUrl({
      comments,
      deploySha,
      fallbackUrl: requestedPreviewCommentUrl,
    });
    const matchingPreview = comments.toReversed().find(
      (comment) =>
        comment.body?.includes(previewMarker) &&
        readMeta(comment.body, 'preview-build-source-sha') ===
          deploySha,
    );
    const firebaseMatch = matchingPreview?.body?.match(
      /^\| \*\*Firebase App Distribution\*\* \|(.*)\|$/m,
    );
    const firebaseStatus =
      firebaseMatch?.[1]?.trim() || '⏳ Awaiting publication';
    const requestDetails = requestCommentId
      ? `@${requestedBy} via \`/deploy\` comment`
      : `@${requestedBy} via workflow dispatch`;
    const buildNumberNote =
      requestedPreviewBuildNumber &&
      deployBuildNumber &&
      requestedPreviewBuildNumber !== deployBuildNumber
        ? '> Deploy build number was bumped to preserve ' +
          'monotonic store build numbers.'
        : null;

    const body = [
      '## 🚀 Preview Deploy',
      '',
      '| | Details |',
      '|---|---|',
      `| **Status** | ${status} |`,
      `| **Requested by** | ${requestDetails} |`,
      deploySha ? `| **Preview SHA** | \`${deploySha}\` |` : null,
      requestedPreviewBuildNumber
        ? '| **Requested Preview Build** | ' +
          `\`${requestedPreviewBuildNumber}\` |`
        : null,
      deployBuildNumber
        ? `| **Deploy Build** | \`${deployBuildNumber}\` |`
        : null,
      `| **Android Status** | ${androidStatus} |`,
      `| **iOS Status** | ${iosStatus} |`,
      `| **Firebase App Distribution** | ${firebaseStatus} |`,
      requestCommentUrl
        ? '| **Request** | ' +
          `[View /deploy comment](${requestCommentUrl}) |`
        : null,
      previewCommentUrl
        ? '| **Preview Comment** | ' +
          `[View preview artifacts](${previewCommentUrl}) |`
        : null,
      requestedAt ? `| **Requested at** | ${requestedAt} |` : null,
      `| **Workflow** | [View deploy run](${runUrl}) |`,
      '',
      buildNumberNote,
      nextStep,
      statusMarker,
      `<!-- preview-deploy-state:${state} -->`,
      '<!-- preview-deploy-last-command-comment-id:' +
        `${requestCommentId} -->`,
      `<!-- preview-deploy-request-key:${requestKey} -->`,
      `<!-- preview-deploy-requested-by:${requestedBy} -->`,
      `<!-- preview-deploy-requested-at:${requestedAt} -->`,
      '<!-- preview-deploy-request-comment-url:' +
        `${requestCommentUrl} -->`,
      '<!-- preview-deploy-preview-comment-url:' +
        `${previewCommentUrl} -->`,
      `<!-- preview-deploy-workflow-url:${runUrl} -->`,
      `<!-- preview-deploy-source-sha:${deploySha} -->`,
      `<!-- preview-deploy-build-number:${deployBuildNumber} -->`,
      '<!-- preview-deploy-requested-build-number:' +
        `${requestedPreviewBuildNumber} -->`,
    ]
      .filter(Boolean)
      .join('\n');

    if (existing) {
      await github.rest.issues.updateComment({
        owner,
        repo,
        comment_id: existing.id,
        body,
      });
    } else {
      await github.rest.issues.createComment({
        owner,
        repo,
        issue_number: issueNumber,
        body,
      });
    }

    return { requestCommentId, body };
}

module.exports = { updateDeployComment };
