// Checks whether the CLA checkbox is ticked in the PR body.

module.exports = async ({ github, context, core }) => {
    const body = context.payload.pull_request.body || '';
    const sha  = context.payload.pull_request.head.sha;

    // Match: - [x] I agree to the [OpenShelves Contributor License Agreement (CLA)]
    const claChecked = /- \[x\] I agree to the \[OpenShelves Contributor License Agreement \(CLA\)\]/i.test(body);

    const status = {
        owner:      context.repo.owner,
        repo:       context.repo.repo,
        sha,
        context:    'CLA / Contributor License Agreement',
        target_url: 'https://github.com/openshelves-labs/openshelves/blob/develop/CLA.md',
    };

    if (claChecked) {
        await github.rest.repos.createCommitStatus({
            ...status,
            state:       'success',
            description: 'CLA accepted. Thank you!',
        });
        core.info('CLA checkbox is checked — status set to success.');
    } else {
        await github.rest.repos.createCommitStatus({
            ...status,
            state:       'failure',
            description: 'Please check the CLA checkbox in your PR description.',
        });
        core.setFailed('CLA checkbox is not checked. PR cannot be merged until the contributor accepts the CLA.');
    }
};
