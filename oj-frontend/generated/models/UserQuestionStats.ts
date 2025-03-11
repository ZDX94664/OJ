/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

export type UserQuestionStats = {
    totalQuestions: number;
    completedQuestions: number;
    submissionStats: {
        success: number;
        failed: number;
    };
    languageStats: {
        [key: string]: number;
    };
    activityStats: {
        dates: string;
        counts: number;
    };
};
