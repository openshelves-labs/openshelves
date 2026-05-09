// Checks whether the PR Checklists checkbox is ticked in the PR body.

module.exports = async ({ github, context, core }) => {
    const body = context.payload.pull_request.body || '';
    const sha  = context.payload.pull_request.head.sha;

    // Match: - [x] I have reviewed the [PR Checklists]
    const checklistChecked = /- \[x\] I have reviewed the \[PR Checklists\]/i.test(body);

    const status = {
        owner:      context.repo.owner,
        repo:       context.repo.repo,
        sha,
        context:    'PR Checks / Quality Checklists',
        target_url: 'https://github.com/openshelves-labs/openshelves/blob/develop/.github/PR_CHECKLISTS.md',
    };

    if (checklistChecked) {
        await github.rest.repos.createCommitStatus({
            ...status,
            state:       'success',
            description: 'PR Checklists reviewed. Thank you!',
        });
        core.info('PR Checklists checkbox is checked — status set to success.');
    } else {
        await github.rest.repos.createCommitStatus({
            ...status,
            state:       'failure',
            description: 'Please check the PR Checklists box in your PR description.',
        });
        core.setFailed('PR Checklists checkbox is not checked. Please review the checklists and tick the box.');
    }
};
